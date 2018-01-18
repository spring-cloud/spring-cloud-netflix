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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.Host;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.context.event.EventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;

/**
 * Route {@link ZuulFilter} that sends requests to predetermined URLs via apache
 * {@link HttpClient}. URLs are found in {@link RequestContext#getRouteHost()}.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Bilal Alp
 */
public class SimpleHostRoutingFilter extends ZuulFilter {

	private static final Log log = LogFactory.getLog(SimpleHostRoutingFilter.class);

	private final Timer connectionManagerTimer = new Timer(
			"SimpleHostRoutingFilter.connectionManagerTimer", true);

	private boolean sslHostnameValidationEnabled;
	private boolean forceOriginalQueryStringEncoding;

	private ProxyRequestHelper helper;
	private Host hostProperties;
	private ApacheHttpClientConnectionManagerFactory connectionManagerFactory;
	private ApacheHttpClientFactory httpClientFactory;
	private HttpClientConnectionManager connectionManager;
	private CloseableHttpClient httpClient;
	private boolean customHttpClient = false;

	@EventListener
	public void onPropertyChange(EnvironmentChangeEvent event) {
		if(!customHttpClient) {
			boolean createNewClient = false;

			for (String key : event.getKeys()) {
				if (key.startsWith("zuul.host.")) {
					createNewClient = true;
					break;
				}
			}

			if (createNewClient) {
				try {
					SimpleHostRoutingFilter.this.httpClient.close();
				} catch (IOException ex) {
					log.error("error closing client", ex);
				}
				SimpleHostRoutingFilter.this.httpClient = newClient();
			}
		}
	}

	public SimpleHostRoutingFilter(ProxyRequestHelper helper, ZuulProperties properties,
			ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
			ApacheHttpClientFactory httpClientFactory) {
		this.helper = helper;
		this.hostProperties = properties.getHost();
		this.sslHostnameValidationEnabled = properties.isSslHostnameValidationEnabled();
		this.forceOriginalQueryStringEncoding = properties
				.isForceOriginalQueryStringEncoding();
		this.connectionManagerFactory = connectionManagerFactory;
		this.httpClientFactory = httpClientFactory;
	}

	public SimpleHostRoutingFilter(ProxyRequestHelper helper, ZuulProperties properties,
								   CloseableHttpClient httpClient) {
		this.helper = helper;
		this.hostProperties = properties.getHost();
		this.sslHostnameValidationEnabled = properties.isSslHostnameValidationEnabled();
		this.forceOriginalQueryStringEncoding = properties
				.isForceOriginalQueryStringEncoding();
		this.httpClient = httpClient;
		this.customHttpClient = true;
	}

	@PostConstruct
	private void initialize() {
		if(!customHttpClient) {
			this.connectionManager = connectionManagerFactory.newConnectionManager(
					!this.sslHostnameValidationEnabled,
					this.hostProperties.getMaxTotalConnections(),
					this.hostProperties.getMaxPerRouteConnections(),
					this.hostProperties.getTimeToLive(), this.hostProperties.getTimeUnit(),
					null);
			this.httpClient = newClient();
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

	protected HttpClientConnectionManager getConnectionManager() {
		return connectionManager;
	}

	protected CloseableHttpClient newClient() {
		final RequestConfig requestConfig = RequestConfig.custom()
				.setSocketTimeout(this.hostProperties.getSocketTimeoutMillis())
				.setConnectTimeout(this.hostProperties.getConnectTimeoutMillis())
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		return httpClientFactory.createBuilder().
				setDefaultRequestConfig(requestConfig).
				setConnectionManager(this.connectionManager).disableRedirectHandling().build();
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

		InputStreamEntity entity = new InputStreamEntity(requestEntity, contentLength,
				contentType);

		HttpRequest httpRequest = buildHttpRequest(verb, uri, entity, headers, params,
				request);
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
