package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RibbonRoutingFilter extends ZuulFilter {

	private static final Logger LOG = LoggerFactory.getLogger(RibbonRoutingFilter.class);

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

	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		return (ctx.getRouteHost() == null && ctx.get("serviceId") != null && ctx
				.sendZuulResponse());
	}

	public Object run() {
		RequestContext context = RequestContext.getCurrentContext();
		HttpServletRequest request = context.getRequest();

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);
		MultiValueMap<String, String> params = helper
				.buildZuulRequestQueryParams(request);
		Verb verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);

		String serviceId = (String) context.get("serviceId");

		RestClient restClient = clientFactory.getClient(serviceId, RestClient.class);

		String uri = request.getRequestURI();
		if (context.get("requestURI") != null) {
			uri = (String) context.get("requestURI");
		}
		// remove double slashes
		uri = uri.replace("//", "/");

		try {
			HttpResponse response = forward(restClient, verb, uri, headers, params,
					requestEntity);
			setResponse(response);
			return response;
		}
		catch (Exception e) {
			context.set("error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			context.set("error.exception", e);
		}
		return null;
	}

	private HttpResponse forward(RestClient restClient, Verb verb, String uri,
			MultiValueMap<String, String> headers, MultiValueMap<String, String> params,
			InputStream requestEntity) throws Exception {

		Map<String, Object> info = helper.debug(verb.verb(), uri, headers, params,
				requestEntity);

		RibbonCommand command = new RibbonCommand(restClient, verb, uri,
				convertHeaders(headers), convertHeaders(params), requestEntity);
		try {
			HttpResponse response = command.execute();
			helper.appendDebug(info, response.getStatus(),
					revertHeaders(response.getHeaders()));
			return response;
		}
		catch (HystrixRuntimeException e) {
			info.put("status", "500");
			if (e.getFallbackException() != null
					&& e.getFallbackException().getCause() != null
					&& e.getFallbackException().getCause() instanceof ClientException) {
				ClientException ex = (ClientException) e.getFallbackException()
						.getCause();
				throw new ZuulException(ex, "Forwarding error", 500, ex.getErrorType()
						.toString());
			}
			throw new ZuulException(e, "Forwarding error", 500, e.getFailureType()
					.toString());
		}

	}

	private MultiValueMap<String, String> revertHeaders(
			Map<String, Collection<String>> headers) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		for (Entry<String, Collection<String>> entry : headers.entrySet()) {
			map.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
		}
		return map;
	}

	private MultivaluedMap<String, String> convertHeaders(
			MultiValueMap<String, String> headers) {
		MultivaluedMap<String, String> map = new MultivaluedMapImpl();
		for (Entry<String, List<String>> entry : headers.entrySet()) {
			map.put(entry.getKey(), entry.getValue());
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
		catch (IOException e) {
			LOG.error("Error during getRequestBody", e);
		}

		return requestEntity;
	}

	Verb getVerb(HttpServletRequest request) {
		String sMethod = request.getMethod();
		return getVerb(sMethod);
	}

	Verb getVerb(String sMethod) {
		if (sMethod == null)
			return Verb.GET;
		sMethod = sMethod.toLowerCase();
		if (sMethod.equals("post"))
			return Verb.POST;
		if (sMethod.equals("put"))
			return Verb.PUT;
		if (sMethod.equals("delete"))
			return Verb.DELETE;
		if (sMethod.equals("options"))
			return Verb.OPTIONS;
		if (sMethod.equals("head"))
			return Verb.HEAD;
		return Verb.GET;
	}

	private void setResponse(HttpResponse resp) throws ClientException, IOException {
		helper.setResponse(resp.getStatus(),
				!resp.hasEntity() ? null : resp.getInputStream(),
				revertHeaders(resp.getHeaders()));
	}

}
