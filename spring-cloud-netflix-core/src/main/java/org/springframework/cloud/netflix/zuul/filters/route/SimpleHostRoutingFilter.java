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

import com.netflix.zuul.context.RequestContext;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
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
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

@CommonsLog
@SuppressWarnings("deprecation")
public class SimpleHostRoutingFilter extends HostRoutingFilter {

	private static final Runnable CLIENTLOADER = new Runnable() {
		@Override
		public void run() {
			loadClient();
		}
	};

	private static final AtomicReference<HttpClient> CLIENT = new AtomicReference<HttpClient>(newClient());

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
					log.error("error closing expired connections", ex);
				}
			}
		}, 30000, 5000);
	}

	public SimpleHostRoutingFilter() {
		super();
	}

	public SimpleHostRoutingFilter(ProxyRequestHelper helper) {
		super(helper);
	}

	@PreDestroy
	public void stop() {
		CONNECTION_MANAGER_TIMER.cancel();
	}

    @Override
	protected HttpResponse forward(String verb, String uri,
			HttpServletRequest request, MultiValueMap<String, String> headers,
			MultiValueMap<String, String> params, InputStream requestEntity)
			throws Exception {
		Map<String, Object> info = this.helper.debug(verb, uri, headers, params,
				requestEntity);
		URL host = RequestContext.getCurrentContext().getRouteHost();
        HttpHost httpHost = new HttpHost(host.getHost(), host.getPort(),
                host.getProtocol());
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
		case "PATCH":
			HttpPatch httpPatch = new HttpPatch(uri + getQueryString());
			httpRequest = httpPatch;
			httpPatch.setEntity(new InputStreamEntity(requestEntity, request
					.getContentLength()));
			break;
		default:
			httpRequest = new BasicHttpRequest(verb, uri + getQueryString());
			log.debug(uri + getQueryString());
		}
		try {
			httpRequest.setHeaders(convertHeaders(headers));
			log.debug(httpHost.getHostName() + " " + httpHost.getPort() + " "
					+ httpHost.getSchemeName());
            HttpClient httpclient = CLIENT.get();
			HttpResponse zuulResponse = httpclient.execute(httpHost, httpRequest);
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

	private String getQueryString() throws UnsupportedEncodingException {
		HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		MultiValueMap<String, String> params=helper.buildZuulRequestQueryParams(request);
		StringBuilder query=new StringBuilder();
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			String key=URLEncoder.encode(entry.getKey(), "UTF-8");
			for (String value : entry.getValue()) {
				query.append("&");
				query.append(key);
				query.append("=");
				query.append(URLEncoder.encode(value, "UTF-8"));
			}
		}
		return (query.length()>0) ? "?" + query.substring(1) : "";
	}

	private static ClientConnectionManager newConnectionManager() throws Exception {
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);
		SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		registry.register(new Scheme("https", sf, 443));
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
						log.error("error shutting down old connection manager", ex);
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

	private static class MySSLSocketFactory extends SSLSocketFactory {
		private SSLContext sslContext = SSLContext.getInstance("TLS");

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
