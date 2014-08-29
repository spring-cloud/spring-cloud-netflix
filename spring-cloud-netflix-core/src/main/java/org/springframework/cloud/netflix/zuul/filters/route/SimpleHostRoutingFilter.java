package org.springframework.cloud.netflix.zuul.filters.route;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.Debug;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

public class SimpleHostRoutingFilter extends ZuulFilter {

	public static final String CONTENT_ENCODING = "Content-Encoding";

	private static final Logger LOG = LoggerFactory
			.getLogger(SimpleHostRoutingFilter.class);
	private static final Runnable CLIENTLOADER = new Runnable() {
		@Override
		public void run() {
			loadClient();
		}
	};

	private static final DynamicIntProperty SOCKET_TIMEOUT = DynamicPropertyFactory
			.getInstance().getIntProperty(ZuulConstants.ZUUL_HOST_SOCKET_TIMEOUT_MILLIS,
					10000);
	private static final DynamicIntProperty CONNECTION_TIMEOUT = DynamicPropertyFactory
			.getInstance().getIntProperty(ZuulConstants.ZUUL_HOST_CONNECT_TIMEOUT_MILLIS,
					2000);

	private static final AtomicReference<HttpClient> CLIENT = new AtomicReference<HttpClient>(
			newClient());

	private static final Timer CONNECTION_MANAGER_TIMER = new Timer(true);

	// cleans expired connections at an interval
	static {
		SOCKET_TIMEOUT.addCallback(CLIENTLOADER);
		CONNECTION_TIMEOUT.addCallback(CLIENTLOADER);
		CONNECTION_MANAGER_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					final HttpClient hc = CLIENT.get();
					if (hc == null)
						return;
					hc.getConnectionManager().closeExpiredConnections();
				}
				catch (Throwable t) {
					LOG.error("error closing expired connections", t);
				}
			}
		}, 30000, 5000);
	}

	public SimpleHostRoutingFilter() {
	}

	private static final ClientConnectionManager newConnectionManager() throws Exception {

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);

		SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		registry.register(new Scheme("https", sf, 443));
		registry.register(new Scheme("https", sf, 8443));

		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(registry);
		cm.setMaxTotal(Integer.parseInt(System.getProperty("zuul.max.host.connections",
				"200")));
		cm.setDefaultMaxPerRoute(Integer.parseInt(System.getProperty(
				"zuul.max.host.connections", "20")));
		return cm;
	}

	@Override
	public String filterType() {
		return "route";
	}

	@Override
	public int filterOrder() {
		return 100;
	}

	public boolean shouldFilter() {
		return RequestContext.getCurrentContext().getRouteHost() != null
				&& RequestContext.getCurrentContext().sendZuulResponse();
	}

	private static final void loadClient() {
		final HttpClient oldClient = CLIENT.get();
		CLIENT.set(newClient());
		if (oldClient != null) {
			CONNECTION_MANAGER_TIMER.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						oldClient.getConnectionManager().shutdown();
					}
					catch (Throwable t) {
						LOG.error("error shutting down old connection manager", t);
					}
				}
			}, 30000);
		}

	}

	private static final HttpClient newClient() {
		// I could statically cache the connection manager but we will probably want to
		// make some of its properties
		// dynamic in the near future also
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient(newConnectionManager());
			HttpParams httpParams = httpclient.getParams();
			httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT,
					SOCKET_TIMEOUT.get());
			httpParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
					CONNECTION_TIMEOUT.get());
			httpclient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0,
					false));
			httpParams.setParameter(ClientPNames.COOKIE_POLICY,
					org.apache.http.client.params.CookiePolicy.IGNORE_COOKIES);
			httpclient.setRedirectStrategy(new org.apache.http.client.RedirectStrategy() {
				@Override
				public boolean isRedirected(HttpRequest httpRequest,
						HttpResponse httpResponse, HttpContext httpContext) {
					return false;
				}

				@Override
				public org.apache.http.client.methods.HttpUriRequest getRedirect(
						HttpRequest httpRequest, HttpResponse httpResponse,
						HttpContext httpContext) {
					return null;
				}
			});
			return httpclient;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Object run() {
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		Header[] headers = buildZuulRequestHeaders(request);
		String verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);
		HttpClient httpclient = CLIENT.get();

		String uri = request.getRequestURI();
		if (RequestContext.getCurrentContext().get("requestURI") != null) {
			uri = (String) RequestContext.getCurrentContext().get("requestURI");
		}

		try {
			HttpResponse response = forward(httpclient, verb, uri, request, headers,
					requestEntity);
			setResponse(response);
		}
		catch (Exception e) {
			if (Debug.debugRequest()) {
				Debug.addRequestDebug("ZUUL:: ERROR " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
		return null;
	}

	private InputStream debug(HttpClient httpclient, String verb, String uri,
			HttpServletRequest request, Header[] headers, InputStream requestEntity)
			throws Exception {

		if (Debug.debugRequest()) {

			Debug.addRequestDebug("ZUUL:: host="
					+ RequestContext.getCurrentContext().getRouteHost());

			for (Header header : headers) {
				Debug.addRequestDebug(String.format("ZUUL::> %s  %s", header.getName(),
						header.getValue()));
			}

			Debug.addRequestDebug(String.format(
					"ZUUL:: > ${verb}  ${uri}?${query} HTTP/1.1", verb, uri,
					request.getQueryString()));
			if (requestEntity != null) {
				requestEntity = debugRequestEntity(requestEntity);
			}

		}
		return requestEntity;
	}

	private InputStream debugRequestEntity(InputStream inputStream) throws Exception {
		if (Debug.debugRequestHeadersOnly())
			return inputStream;
		if (inputStream == null)
			return null;
		String entity = IOUtils.toString(inputStream);
		Debug.addRequestDebug("ZUUL::> " + entity);
		return new ByteArrayInputStream(entity.getBytes());
	}

	private HttpResponse forward(HttpClient httpclient, String verb, String uri,
			HttpServletRequest request, Header[] headers, InputStream requestEntity)
			throws Exception {

		requestEntity = debug(httpclient, verb, uri, request, headers, requestEntity);

		HttpHost httpHost = getHttpHost();

		HttpRequest httpRequest;

		switch (verb) {
		case "POST":
			HttpPost httpPost = new HttpPost(uri + getQueryString());
			httpRequest = httpPost;
			httpPost.setEntity(new InputStreamEntity(requestEntity, request
					.getContentLength()));
			break;
		case "PUT":
			HttpPut httpPut = new HttpPut(uri + getQueryString());
			httpRequest = httpPut;
			httpPut.setEntity(new InputStreamEntity(requestEntity, request
					.getContentLength()));
			break;
		default:
			httpRequest = new BasicHttpRequest(verb, uri + getQueryString());
			LOG.debug(uri + getQueryString());
		}

		try {
			httpRequest.setHeaders(headers);
			LOG.debug(httpHost.getHostName() + " " + httpHost.getPort() + " "
					+ httpHost.getSchemeName());
			HttpResponse zuulResponse = forwardRequest(httpclient, httpHost, httpRequest);
			return zuulResponse;
		}
		finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			// httpclient.getConnectionManager().shutdown();
		}

	}

	private HttpResponse forwardRequest(HttpClient httpclient, HttpHost httpHost,
			HttpRequest httpRequest) throws IOException {
		return httpclient.execute(httpHost, httpRequest);
	}

	String getQueryString() {
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		String query = request.getQueryString();
		return (query != null) ? "?" + query : "";
	}

	HttpHost getHttpHost() {
		URL host = RequestContext.getCurrentContext().getRouteHost();

		HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(),
				host.getProtocol());

		return httpHost;
	}

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = request.getInputStream();
		}
		catch (IOException e) {
			// no requestBody is ok.
		}
		return requestEntity;
	}

	boolean isValidHeader(String name) {
		if (name.toLowerCase().contains("content-length"))
			return false;
		if (!RequestContext.getCurrentContext().getResponseGZipped()) {
			if (name.toLowerCase().contains("accept-encoding"))
				return false;
		}
		return true;
	}

	private Header[] buildZuulRequestHeaders(HttpServletRequest request) {

		ArrayList<Header> headers = new ArrayList<>();
		Enumeration headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = (String) headerNames.nextElement();
			String value = request.getHeader(name);
			if (isValidHeader(name))
				headers.add(new BasicHeader(name, value));
		}

		Map<String, String> zuulRequestHeaders = RequestContext.getCurrentContext()
				.getZuulRequestHeaders();

		for (String it : zuulRequestHeaders.keySet()) {
			final String name = it.toLowerCase();
			Optional<Header> h = Iterables.tryFind(headers, new Predicate<Header>() {
				@Override
				public boolean apply(@Nullable Header input) {
					return input.getName().equals(name);
				}
			});
			if (h.isPresent()) {
				headers.remove(h);
			}
			headers.add(new BasicHeader(it, zuulRequestHeaders.get(it)));
		}

		if (RequestContext.getCurrentContext().getResponseGZipped()) {
			headers.add(new BasicHeader("accept-encoding", "deflate, gzip"));
		}
		return headers.toArray(new Header[0]);
	}

	private String getVerb(HttpServletRequest request) {
		String sMethod = request.getMethod();
		return sMethod.toUpperCase();
	}

	private String getVerb(String sMethod) {
		if (sMethod == null)
			return "GET";
		sMethod = sMethod.toLowerCase();
		if (sMethod.equalsIgnoreCase("post"))
			return "POST";
		if (sMethod.equalsIgnoreCase("put"))
			return "PUT";
		if (sMethod.equalsIgnoreCase("delete"))
			return "DELETE";
		if (sMethod.equalsIgnoreCase("options"))
			return "OPTIONS";
		if (sMethod.equalsIgnoreCase("head"))
			return "HEAD";
		return "GET";
	}

	private void setResponse(HttpResponse response) throws IOException {
		RequestContext context = RequestContext.getCurrentContext();

		RequestContext.getCurrentContext().set("hostZuulResponse", response);
		RequestContext.getCurrentContext().setResponseStatusCode(
				response.getStatusLine().getStatusCode());
		if (response.getEntity() != null) {
			RequestContext.getCurrentContext().setResponseDataStream(
					response.getEntity().getContent());
		}

		boolean isOriginResponseGzipped = false;

		for (Header h : response.getHeaders(CONTENT_ENCODING)) {
			if (HTTPRequestUtils.getInstance().isGzipped(h.getValue())) {
				isOriginResponseGzipped = true;
				break;
			}
		}
		context.setResponseGZipped(isOriginResponseGzipped);

		if (Debug.debugRequest()) {
			for (Header header : response.getAllHeaders()) {
				if (isValidHeader(header)) {
					RequestContext.getCurrentContext().addZuulResponseHeader(
							header.getName(), header.getValue());
					Debug.addRequestDebug("ORIGIN_RESPONSE:: < " + header.getName() + ","
							+ header.getValue());
				}
			}

			if (context.getResponseDataStream() != null) {
				byte[] origBytes = IOUtils.toByteArray(context.getResponseDataStream());
				ByteArrayInputStream byteStream = new ByteArrayInputStream(origBytes);
				InputStream inputStream = byteStream;
				if (RequestContext.getCurrentContext().getResponseGZipped()) {
					inputStream = new GZIPInputStream(byteStream);
				}

				context.setResponseDataStream(new ByteArrayInputStream(origBytes));
			}

		}
		else {
			for (Header header : response.getAllHeaders()) {
				RequestContext ctx = RequestContext.getCurrentContext();
				ctx.addOriginResponseHeader(header.getName(), header.getValue());

				if (header.getName().equalsIgnoreCase("content-length"))
					ctx.setOriginContentLength(header.getValue());

				if (isValidHeader(header)) {
					ctx.addZuulResponseHeader(header.getName(), header.getValue());
				}
			}
		}

	}

	boolean isValidHeader(Header header) {
		switch (header.getName().toLowerCase()) {
		case "connection":
		case "content-length":
		case "content-encoding":
		case "server":
		case "transfer-encoding":
			return false;
		default:
			return true;
		}
	}

	public static class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
				KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			TrustManager[] tms = new TrustManager[1];
			tms[0] = tm;
			sslContext.init(null, tms, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
				throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port,
					autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}
}