/*
 * Copyright 2013-2017 the original author or authors.
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
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.Host;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTPS_SCHEME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.HTTP_SCHEME;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;

/**
 * Route {@link ZuulFilter} that sends requests to predetermined URLs via apache {@link HttpClient}.
 * URLs are found in {@link RequestContext#getRouteHost()}.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Bilal Alp
 */
public class SimpleHostRoutingFilter extends ZuulFilter {

	private static final Log log = LogFactory.getLog(SimpleHostRoutingFilter.class);

	private static final DynamicIntProperty SOCKET_TIMEOUT = DynamicPropertyFactory
			.getInstance()
			.getIntProperty(ZuulConstants.ZUUL_HOST_SOCKET_TIMEOUT_MILLIS, 10000);

	private static final DynamicIntProperty CONNECTION_TIMEOUT = DynamicPropertyFactory
			.getInstance()
			.getIntProperty(ZuulConstants.ZUUL_HOST_CONNECT_TIMEOUT_MILLIS, 2000);

	private final Timer connectionManagerTimer = new Timer(
			"SimpleHostRoutingFilter.connectionManagerTimer", true);

	private boolean sslHostnameValidationEnabled;
	private boolean forceOriginalQueryStringEncoding;

	private ProxyRequestHelper helper;
	private Host hostProperties;
	private PoolingHttpClientConnectionManager connectionManager;
	private CloseableHttpClient httpClient;

	private final Runnable clientloader = new Runnable() {
		@Override
		public void run() {
			try {
				SimpleHostRoutingFilter.this.httpClient.close();
			}
			catch (IOException ex) {
				log.error("error closing client", ex);
			}
			SimpleHostRoutingFilter.this.httpClient = newClient();
		}
	};

	public SimpleHostRoutingFilter(ProxyRequestHelper helper, ZuulProperties properties) {
		this.helper = helper;
		this.hostProperties = properties.getHost();
		this.sslHostnameValidationEnabled = properties.isSslHostnameValidationEnabled();
		this.forceOriginalQueryStringEncoding = properties
				.isForceOriginalQueryStringEncoding();
	}

	@PostConstruct
	private void initialize() {
		this.httpClient = newClient();
		SOCKET_TIMEOUT.addCallback(this.clientloader);
		CONNECTION_TIMEOUT.addCallback(this.clientloader);
		this.connectionManagerTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (SimpleHostRoutingFilter.this.connectionManager == null) {
					return;
				}
				SimpleHostRoutingFilter.this.connectionManager.closeExpiredConnections();
			}
		}, 30000, 5000);
	}

	@PreDestroy
	public void stop() {
		this.connectionManagerTimer.cancel();
	}

	@Override
	public String filterType() {
		return ROUTE_TYPE;
	}

	@Override
	public int filterOrder() {
		return SIMPLE_HOST_ROUTING_FILTER_ORDER;
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
		if (request.getContentLength() < 0) {
			context.setChunkedRequestBody();
		}

		String uri = this.helper.buildZuulRequestURI(request);
		this.helper.addIgnoredHeaders();

		try {
			CloseableHttpResponse response = forward(this.httpClient, verb, uri, request,
					headers, params, requestEntity);
			setResponse(response);
		}
		catch (Exception ex) {
			throw new ZuulRuntimeException(ex);
		}
		return null;
	}

	protected PoolingHttpClientConnectionManager newConnectionManager() {
		try {
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] x509Certificates,
						String s) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] x509Certificates,
						String s) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			} }, new SecureRandom());

			RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder
					.<ConnectionSocketFactory> create()
					.register(HTTP_SCHEME, PlainConnectionSocketFactory.INSTANCE);
			if (this.sslHostnameValidationEnabled) {
				registryBuilder.register(HTTPS_SCHEME,
						new SSLConnectionSocketFactory(sslContext));
			}
			else {
				registryBuilder.register(HTTPS_SCHEME, new SSLConnectionSocketFactory(
						sslContext, NoopHostnameVerifier.INSTANCE));
			}
			final Registry<ConnectionSocketFactory> registry = registryBuilder.build();

			this.connectionManager = new PoolingHttpClientConnectionManager(registry, null, null, null,
					hostProperties.getTimeToLive(), hostProperties.getTimeUnit());
			this.connectionManager
					.setMaxTotal(this.hostProperties.getMaxTotalConnections());
			this.connectionManager.setDefaultMaxPerRoute(
					this.hostProperties.getMaxPerRouteConnections());
			return this.connectionManager;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	protected CloseableHttpClient newClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(SOCKET_TIMEOUT.get())
				.setConnectTimeout(CONNECTION_TIMEOUT.get())
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();

		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		if (!this.sslHostnameValidationEnabled) {
			httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
		}
		return httpClientBuilder.setConnectionManager(newConnectionManager())
				.disableContentCompression()
				.useSystemProperties().setDefaultRequestConfig(requestConfig)
				.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
				.setRedirectStrategy(new RedirectStrategy() {
					@Override
					public boolean isRedirected(HttpRequest request,
							HttpResponse response, HttpContext context)
							throws ProtocolException {
						return false;
					}

					@Override
					public HttpUriRequest getRedirect(HttpRequest request,
							HttpResponse response, HttpContext context)
							throws ProtocolException {
						return null;
					}
				}).build();
	}

	private CloseableHttpResponse forward(CloseableHttpClient httpclient, String verb,
			String uri, HttpServletRequest request, MultiValueMap<String, String> headers,
			MultiValueMap<String, String> params, InputStream requestEntity)
			throws Exception {
		Map<String, Object> info = this.helper.debug(verb, uri, headers, params,
				requestEntity);
		URL host = RequestContext.getCurrentContext().getRouteHost();
		HttpHost httpHost = getHttpHost(host);
		uri = StringUtils.cleanPath((host.getPath() + uri).replaceAll("/{2,}", "/"));
		int contentLength = request.getContentLength();

		ContentType contentType = null;

		if (request.getContentType() != null) {
			contentType = ContentType.parse(request.getContentType());
		}

		InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength, contentType);

		HttpRequest httpRequest = buildHttpRequest(verb, uri, entity, headers, params, request);
		try {
			log.debug(httpHost.getHostName() + " " + httpHost.getPort() + " "
					+ httpHost.getSchemeName());
			CloseableHttpResponse zuulResponse = forwardRequest(httpclient, httpHost,
					httpRequest);
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

	protected HttpRequest buildHttpRequest(String verb, String uri,
			InputStreamEntity entity, MultiValueMap<String, String> headers,
			MultiValueMap<String, String> params, HttpServletRequest request) {
		HttpRequest httpRequest;
		String uriWithQueryString = uri + (this.forceOriginalQueryStringEncoding
				? getEncodedQueryString(request) : this.helper.getQueryString(params));

		switch (verb.toUpperCase()) {
			case "POST":
				HttpPost httpPost = new HttpPost(uriWithQueryString);
				httpRequest = httpPost;
				httpPost.setEntity(entity);
				break;
			case "PUT":
				HttpPut httpPut = new HttpPut(uriWithQueryString);
				httpRequest = httpPut;
				httpPut.setEntity(entity);
				break;
			case "PATCH":
				HttpPatch httpPatch = new HttpPatch(uriWithQueryString);
				httpRequest = httpPatch;
				httpPatch.setEntity(entity);
				break;
			case "DELETE":
				BasicHttpEntityEnclosingRequest entityRequest = new BasicHttpEntityEnclosingRequest(
						verb, uriWithQueryString);
				httpRequest = entityRequest;
				entityRequest.setEntity(entity);
				break;
			default:
				httpRequest = new BasicHttpRequest(verb, uriWithQueryString);
				log.debug(uriWithQueryString);
		}

		httpRequest.setHeaders(convertHeaders(headers));
		return httpRequest;
	}

	private String getEncodedQueryString(HttpServletRequest request) {
		String query = request.getQueryString();
		return (query != null) ? "?" + query : "";
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

	private CloseableHttpResponse forwardRequest(CloseableHttpClient httpclient,
			HttpHost httpHost, HttpRequest httpRequest) throws IOException {
		return httpclient.execute(httpHost, httpRequest);
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
		RequestContext.getCurrentContext().set("zuulResponse", response);
		this.helper.setResponse(response.getStatusLine().getStatusCode(),
				response.getEntity() == null ? null : response.getEntity().getContent(),
				revertHeaders(response.getAllHeaders()));
	}

	/**
	 * Add header names to exclude from proxied response in the current request.
	 * @param names
	 */
	protected void addIgnoredHeaders(String... names) {
		this.helper.addIgnoredHeaders(names);
	}

	/**
	 * Determines whether the filter enables the validation for ssl hostnames.
	 * @return true if enabled
	 */
	boolean isSslHostnameValidationEnabled() {
		return this.sslHostnameValidationEnabled;
	}
}
