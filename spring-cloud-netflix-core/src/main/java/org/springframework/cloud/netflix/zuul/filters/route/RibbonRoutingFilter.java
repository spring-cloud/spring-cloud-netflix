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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.netflix.client.ClientException;
import com.netflix.client.http.HttpRequest.Verb;
import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.niws.client.http.RestClient;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

@CommonsLog
public class RibbonRoutingFilter extends ZuulFilter {

	public static final String CONTENT_ENCODING = "Content-Encoding";

	private SpringClientFactory clientFactory;

	private ProxyRequestHelper helper;

	public RibbonRoutingFilter(ProxyRequestHelper helper,
			SpringClientFactory clientFactory) {
		this.helper = helper;
		this.clientFactory = clientFactory;
	}

	public RibbonRoutingFilter(SpringClientFactory clientFactory) {
		this(new ProxyRequestHelper(), clientFactory);
	}

	@Override
	public String filterType() {
		return "route";
	}

	@Override
	public int filterOrder() {
		return 10;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return (ctx.getRouteHost() == null && ctx.get("serviceId") != null && ctx
				.sendZuulResponse());
	}

	@Override
	public Object run() {
		RequestContext context = RequestContext.getCurrentContext();
		HttpServletRequest request = context.getRequest();

		MultiValueMap<String, String> headers = this.helper
				.buildZuulRequestHeaders(request);
		MultiValueMap<String, String> params = this.helper
				.buildZuulRequestQueryParams(request);
		Verb verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);

		String serviceId = (String) context.get("serviceId");
		Boolean retryable = (Boolean) context.get("retryable");

		RestClient restClient = this.clientFactory.getClient(serviceId, RestClient.class);

		String uri = this.helper.buildZuulRequestURI(request);

		// remove double slashes
		uri = uri.replace("//", "/");
		String service = (String) context.get("serviceId");

		try {
			HttpResponse response = forward(restClient, service, verb, uri, retryable, headers, params,
					requestEntity);
			setResponse(response);
			return response;
		}
		catch (Exception ex) {
			context.set("error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			context.set("error.exception", ex);
		}
		return null;
	}

	private HttpResponse forward(RestClient restClient, String service, Verb verb, String uri, Boolean retryable,
			MultiValueMap<String, String> headers, MultiValueMap<String, String> params,
			InputStream requestEntity) throws Exception {
		Map<String, Object> info = this.helper.debug(verb.verb(), uri, headers, params,
				requestEntity);
		RibbonCommand command = new RibbonCommand(service, restClient, verb, uri, retryable,
				headers, params, requestEntity);
		try {
			HttpResponse response = command.execute();
			this.helper.appendDebug(info, response.getStatus(),
					revertHeaders(response.getHeaders()));
			return response;
		}
		catch (HystrixRuntimeException ex) {
			info.put("status", "500");
			if (ex.getFallbackException() != null
					&& ex.getFallbackException().getCause() != null
					&& ex.getFallbackException().getCause() instanceof ClientException) {
				ClientException cause = (ClientException) ex.getFallbackException()
						.getCause();
				throw new ZuulException(cause, "Forwarding error", 500, cause
						.getErrorType().toString());
			}
			throw new ZuulException(ex, "Forwarding error", 500, ex.getFailureType()
					.toString());
		}

	}

	private MultiValueMap<String, String> revertHeaders(
			Map<String, Collection<String>> headers) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		for (Entry<String, Collection<String>> entry : headers.entrySet()) {
			map.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return map;
	}

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		// ApacheHttpClient4Handler does not support body in delete requests
		if (request.getMethod().equals("DELETE")) {
			return null;
		}
		try {
			requestEntity = (InputStream) RequestContext.getCurrentContext().get(
					"requestEntity");
			if (requestEntity == null) {
				requestEntity = request.getInputStream();
			}
		}
		catch (IOException ex) {
			log.error("Error during getRequestBody", ex);
		}
		return requestEntity;
	}

	private Verb getVerb(HttpServletRequest request) {
		String sMethod = request.getMethod();
		return getVerb(sMethod);
	}

	private Verb getVerb(String sMethod) {
		if (sMethod == null)
			return Verb.GET;
		try {
			return Verb.valueOf(sMethod.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return Verb.GET;
		}
	}

	private void setResponse(HttpResponse resp) throws ClientException, IOException {
		this.helper.setResponse(resp.getStatus(),
				!resp.hasEntity() ? null : resp.getInputStream(),
				revertHeaders(resp.getHeaders()));
	}

}
