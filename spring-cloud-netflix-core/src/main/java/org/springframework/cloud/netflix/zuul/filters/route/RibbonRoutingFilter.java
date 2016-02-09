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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.client.ClientException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class RibbonRoutingFilter extends ZuulFilter {

	private ProxyRequestHelper helper;
	private RibbonCommandFactory<?> ribbonCommandFactory;

	public RibbonRoutingFilter(ProxyRequestHelper helper,
			RibbonCommandFactory<?> ribbonCommandFactory) {
		this.helper = helper;
		this.ribbonCommandFactory = ribbonCommandFactory;
	}

	public RibbonRoutingFilter(RibbonCommandFactory<?> ribbonCommandFactory) {
		this(new ProxyRequestHelper(), ribbonCommandFactory);
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
		return (ctx.getRouteHost() == null && ctx.get("serviceId") != null
				&& ctx.sendZuulResponse());
	}

	@Override
	public Object run() {
		RequestContext context = RequestContext.getCurrentContext();
		try {
			RibbonCommandContext commandContext = buildCommandContext(context);
			ClientHttpResponse response = forward(commandContext);
			setResponse(response);
			return response;
		}
		catch (ZuulException ex) {
			context.set("error.status_code", ex.nStatusCode);
			context.set("error.message", ex.errorCause);
			context.set("error.exception", ex);
		}
		catch (Exception ex) {
			context.set("error.status_code",
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			context.set("error.exception", ex);
		}
		return null;
	}

	private RibbonCommandContext buildCommandContext(RequestContext context) {
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

		String serviceId = (String) context.get("serviceId");
		Boolean retryable = (Boolean) context.get("retryable");

		String uri = this.helper.buildZuulRequestURI(request);

		// remove double slashes
		uri = uri.replace("//", "/");

		return new RibbonCommandContext(serviceId, verb, uri, retryable, headers, params,
				requestEntity);
	}

	private ClientHttpResponse forward(RibbonCommandContext context) throws Exception {
		Map<String, Object> info = this.helper.debug(context.getVerb(), context.getUri(),
				context.getHeaders(), context.getParams(), context.getRequestEntity());

		RibbonCommand command = this.ribbonCommandFactory.create(context);
		try {
			ClientHttpResponse response = command.execute();
			this.helper.appendDebug(info, response.getStatusCode().value(),
					response.getHeaders());
			return response;
		}
		catch (HystrixRuntimeException ex) {
			info.put("status", "500");
			ClientException clientException = findClientException(ex);
			if (clientException != null) {
				int statusCode = 500;
				if (clientException.getErrorType() == ClientException.ErrorType.SERVER_THROTTLED) {
					statusCode = 503;
				}
				throw new ZuulException(clientException, "Forwarding error", statusCode,
						clientException.getErrorType().toString());
			}
			throw new ZuulException(ex, "Forwarding error", 500,
					ex.getFailureType().toString());
		}

	}

	protected ClientException findClientException(HystrixRuntimeException ex) {
		if (ex.getCause() != null
			&& ex.getCause() instanceof ClientException) {
			return (ClientException) ex.getCause();
		}
		if (ex.getFallbackException() != null
				&& ex.getFallbackException().getCause() != null
				&& ex.getFallbackException().getCause() instanceof ClientException) {
			return (ClientException) ex.getFallbackException().getCause();
		}
		return null;
	}

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		// ApacheHttpClient4Handler does not support body in delete requests
		if (request.getMethod().equals("DELETE")) {
			return null;
		}
		try {
			requestEntity = (InputStream) RequestContext.getCurrentContext()
					.get("requestEntity");
			if (requestEntity == null) {
				requestEntity = request.getInputStream();
			}
		}
		catch (IOException ex) {
			log.error("Error during getRequestBody", ex);
		}
		return requestEntity;
	}

	private String getVerb(HttpServletRequest request) {
		String method = request.getMethod();
		if (method == null) {
			return "GET";
		}
		return method;
	}

	private void setResponse(ClientHttpResponse resp)
			throws ClientException, IOException {
		this.helper.setResponse(resp.getStatusCode().value(),
				resp.getBody() == null ? null : resp.getBody(), resp.getHeaders());
	}

}
