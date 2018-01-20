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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;

import com.netflix.client.ClientException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_ENTITY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RETRYABLE_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RIBBON_ROUTING_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.LOAD_BALANCER_KEY;

/**
 * Route {@link ZuulFilter} that uses Ribbon, Hystrix and pluggable http clients to send requests.
 * ServiceIds are found in the {@link RequestContext} attribute {@link org.springframework.cloud.netflix.zuul.filters.support.FilterConstants#SERVICE_ID_KEY}.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Ryan Baxter
 */
public class RibbonRoutingFilter extends ZuulFilter {

	private static final Log log = LogFactory.getLog(RibbonRoutingFilter.class);

	protected ProxyRequestHelper helper;
	protected RibbonCommandFactory<?> ribbonCommandFactory;
	protected List<RibbonRequestCustomizer> requestCustomizers;
	private boolean useServlet31 = true;

	public RibbonRoutingFilter(ProxyRequestHelper helper,
							   RibbonCommandFactory<?> ribbonCommandFactory,
							   List<RibbonRequestCustomizer> requestCustomizers) {
		this.helper = helper;
		this.ribbonCommandFactory = ribbonCommandFactory;
		this.requestCustomizers = requestCustomizers;
		// To support Servlet API 3.1 we need to check if getContentLengthLong exists
		try {
			//TODO: remove in 2.0
			HttpServletRequest.class.getMethod("getContentLengthLong");
		} catch(NoSuchMethodException e) {
			useServlet31 = false;
		}
	}

	public RibbonRoutingFilter(RibbonCommandFactory<?> ribbonCommandFactory) {
		this(new ProxyRequestHelper(), ribbonCommandFactory, null);
	}

	/* for testing */ boolean isUseServlet31() {
		return useServlet31;
	}

	@Override
	public String filterType() {
		return ROUTE_TYPE;
	}

	@Override
	public int filterOrder() {
		return RIBBON_ROUTING_FILTER_ORDER;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return (ctx.getRouteHost() == null && ctx.get(SERVICE_ID_KEY) != null
				&& ctx.sendZuulResponse());
	}

	@Override
	public Object run() {
		RequestContext context = RequestContext.getCurrentContext();
		this.helper.addIgnoredHeaders();
		try {
			RibbonCommandContext commandContext = buildCommandContext(context);
			ClientHttpResponse response = forward(commandContext);
			setResponse(response);
			return response;
		}
		catch (ZuulException ex) {
			throw new ZuulRuntimeException(ex);
		}
		catch (Exception ex) {
			throw new ZuulRuntimeException(ex);
		}
	}

	protected RibbonCommandContext buildCommandContext(RequestContext context) {
		HttpServletRequest request = context.getRequest();

		MultiValueMap<String, String> headers = this.helper
				.buildZuulRequestHeaders(request);
		MultiValueMap<String, String> params = this.helper
				.buildZuulRequestQueryParams(request);
		String verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);
		if (request.getContentLength() < 0 && !verb.equalsIgnoreCase("GET")) {
			context.setChunkedRequestBody();
		}

		String serviceId = (String) context.get(SERVICE_ID_KEY);
		Boolean retryable = (Boolean) context.get(RETRYABLE_KEY);
		Object loadBalancerKey = context.get(LOAD_BALANCER_KEY);

		String uri = this.helper.buildZuulRequestURI(request);

		// remove double slashes
		uri = uri.replace("//", "/");

		long contentLength = useServlet31 ? request.getContentLengthLong(): request.getContentLength();

		return new RibbonCommandContext(serviceId, verb, uri, retryable, headers, params,
				requestEntity, this.requestCustomizers, contentLength, loadBalancerKey);
	}

	protected ClientHttpResponse forward(RibbonCommandContext context) throws Exception {
		Map<String, Object> info = this.helper.debug(context.getMethod(),
				context.getUri(), context.getHeaders(), context.getParams(),
				context.getRequestEntity());

		RibbonCommand command = this.ribbonCommandFactory.create(context);
		try {
			ClientHttpResponse response = command.execute();
			this.helper.appendDebug(info, response.getRawStatusCode(), response.getHeaders());
			return response;
		}
		catch (HystrixRuntimeException ex) {
			return handleException(info, ex);
		}

	}

	protected ClientHttpResponse handleException(Map<String, Object> info,
			HystrixRuntimeException ex) throws ZuulException {
		int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
		Throwable cause = ex;
		String message = ex.getFailureType().toString();

		ClientException clientException = findClientException(ex);
		if (clientException == null) {
			clientException = findClientException(ex.getFallbackException());
		}

		if (clientException != null) {
			if (clientException
					.getErrorType() == ClientException.ErrorType.SERVER_THROTTLED) {
				statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
			}
			cause = clientException;
			message = clientException.getErrorType().toString();
		}
		info.put("status", String.valueOf(statusCode));
		throw new ZuulException(cause, "Forwarding error", statusCode, message);
	}

	protected ClientException findClientException(Throwable t) {
		if (t == null) {
			return null;
		}
		if (t instanceof ClientException) {
			return (ClientException) t;
		}
		return findClientException(t.getCause());
	}

	protected InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = (InputStream) RequestContext.getCurrentContext()
					.get(REQUEST_ENTITY_KEY);
			if (requestEntity == null) {
				requestEntity = request.getInputStream();
			}
		}
		catch (IOException ex) {
			log.error("Error during getRequestBody", ex);
		}
		return requestEntity;
	}

	protected String getVerb(HttpServletRequest request) {
		String method = request.getMethod();
		if (method == null) {
			return "GET";
		}
		return method;
	}

	protected void setResponse(ClientHttpResponse resp)
			throws ClientException, IOException {
		RequestContext.getCurrentContext().set("zuulResponse", resp);
		this.helper.setResponse(resp.getRawStatusCode(),
				resp.getBody() == null ? null : resp.getBody(), resp.getHeaders());
	}

}
