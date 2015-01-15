/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;

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

	private static final Timer CONNECTION_MANAGER_TIMER = new Timer(
			"SimpleHostRoutingFilter.CONNECTION_MANAGER_TIMER", true);

	// cleans expired connections at an interval
	static {
		SOCKET_TIMEOUT.addCallback(CLIENTLOADER);
		CONNECTION_TIMEOUT.addCallback(CLIENTLOADER);
		CONNECTION_MANAGER_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					final HttpClient hc = CLIENT.get();
					if (hc == null) {
						return;
					}
					hc.getConnectionManager().closeExpiredConnections();
				}
				catch (Throwable ex) {
					LOG.error("error closing expired connections", ex);
				}
			}
		}, 30000, 5000);
	}

	private ProxyRequestHelper helper;

	public SimpleHostRoutingFilter() {
		this(new ProxyRequestHelper());
	}

	public SimpleHostRoutingFilter(ProxyRequestHelper helper) {
		this.helper = helper;
	}

	@PreDestroy
	public void stop() {
		CONNECTION_MANAGER_TIMER.cancel();
	}

	@Override
	public String filterType() {
		return "route";
	}

	@Override
	public int filterOrder() {
		return 100;
	}

	@Override
	public boolean shouldFilter() {
		return RequestContext.getCurrentContext().getRouteHost() != null
				&& RequestContext.getCurrentContext().sendZuulResponse();
	}

	@Override
	public Object run() {
		RequestContext context = RequestContext.getCurrentContext();
		HttpServletRequest request = context.getRequest();
		MultiValueMap<String, String> headers = this.helper
				.buildZuulRequestHeaders(request);
		MultiValueMap<String, String> params = this.helper
				.buildZuulRequestQueryParams(request);
		String verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);
		HttpClient httpclient = CLIENT.get();

		String uri = request.getRequestURI();
		if (context.get("requestURI") != null) {
			uri = (String) context.get("requestURI");
		}

		try {
			HttpResponse response = forward(httpclient, verb, uri, request, headers,
					params, requestEntity);
			setResponse(response);
		}
		catch (Exception ex) {
			context.set("error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			context.set("error.exception", ex);
		}
		return null;
	}

	private HttpResponse forward(HttpClient httpclient, String verb, String uri,
			HttpServletRequest request, MultiValueMap<String, String> headers,
			MultiValueMap<String, String> params, InputStream requestEntity)
			throws Exception {
		Map<String, Object> info = this.helper.debug(verb, uri, headers, params,
				requestEntity);
		URL host = RequestContext.getCurrentContext().getRouteHost();
		HttpHost httpHost = getHttpHost(host);
		uri = StringUtils.cleanPath(host.getPath() + uri);
		HttpRequest httpRequest;
		switch (verb.toUpperCase()) {
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
			httpRequest.setHeaders(convertHeaders(headers));
			LOG.debug(httpHost.getHostName() + " " + httpHost.getPort() + " "
					+ httpHost.getSchemeName());
			HttpResponse zuulResponse = forwardRequest(httpclient, httpHost, httpRequest);
			this.helper.appendDebug(info, zuulResponse.getStatusLine().getStatusCode(),
					revertHeaders(zuulResponse.getAllHeaders()));
			return zuulResponse;
		}
		finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			// httpclient.getConnectionManager().shutdown();
		}
	}

	private MultiValueMap<String, String> revertHeaders(Header[] headers) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		for (Header header : headers) {
			String name = header.getName();
			if (!map.containsKey(name)) {
				map.put(name, new ArrayList<String>());
			}
			map.get(name).add(header.getValue());
		}
		return map;
	}

	private Header[] convertHeaders(MultiValueMap<String, String> headers) {
		List<Header> list = new ArrayList<>();
		for (String name : headers.keySet()) {
			for (String value : headers.get(name)) {
				list.add(new BasicHeader(name, value));
			}
		}
		return list.toArray(new BasicHeader[0]);
	}

	private HttpResponse forwardRequest(HttpClient httpclient, HttpHost httpHost,
			HttpRequest httpRequest) throws IOException {
		return httpclient.execute(httpHost, httpRequest);
	}

	private String getQueryString() {
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		String query = request.getQueryString();
		return (query != null) ? "?" + query : "";
	}

	private HttpHost getHttpHost(URL host) {
		HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(),
				host.getProtocol());
		return httpHost;
	}

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = request.getInputStream();
		}
		catch (IOException ex) {
			// no requestBody is ok.
		}
		return requestEntity;
	}

	private String getVerb(HttpServletRequest request) {
		String sMethod = request.getMethod();
		return sMethod.toUpperCase();
	}

	private void setResponse(HttpResponse response) throws IOException {
		this.helper.setResponse(response.getStatusLine().getStatusCode(),
				response.getEntity() == null ? null : response.getEntity().getContent(),
				revertHeaders(response.getAllHeaders()));
	}

	private static ClientConnectionManager newConnectionManager() throws Exception {
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

	private static void loadClient() {
		final HttpClient oldClient = CLIENT.get();
		CLIENT.set(newClient());
		if (oldClient != null) {
			CONNECTION_MANAGER_TIMER.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						oldClient.getConnectionManager().shutdown();
					}
					catch (Throwable ex) {
						LOG.error("error shutting down old connection manager", ex);
					}
				}
			}, 30000);
		}
	}

	private static HttpClient newClient() {
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
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
				KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);
			TrustManager tm = new X509TrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

			};
			TrustManager[] tms = new TrustManager[1];
			tms[0] = tm;
			this.sslContext.init(null, tms, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
				throws IOException, UnknownHostException {
			return this.sslContext.getSocketFactory().createSocket(socket, host, port,
					autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return this.sslContext.getSocketFactory().createSocket();
		}

	}

}
