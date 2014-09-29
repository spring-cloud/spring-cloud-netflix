package org.springframework.cloud.netflix.zuul.filters.route;

import static org.springframework.cloud.netflix.eureka.EurekaRibbonInitializer.setServiceListClassAndVIP;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.cloud.netflix.zuul.RibbonCommand;
import org.springframework.util.StringUtils;

import com.netflix.client.ClientException;
import com.netflix.client.ClientFactory;
import com.netflix.client.http.HttpRequest.Verb;
import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.niws.client.http.RestClient;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.util.HTTPRequestUtils;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RibbonRoutingFilter extends ZuulFilter {

	private static final Logger LOG = LoggerFactory.getLogger(RibbonRoutingFilter.class);

	public static final String CONTENT_ENCODING = "Content-Encoding";

	private TraceRepository traces;

	public void setTraces(TraceRepository traces) {
		this.traces = traces;
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

		MultivaluedMap<String, String> headers = buildZuulRequestHeaders(request);
		MultivaluedMap<String, String> params = buildZuulRequestQueryParams(request);
		Verb verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);

		String serviceId = (String) context.get("serviceId");

		// TODO: can this be set be default? or an implementation of an interface?
		setServiceListClassAndVIP(serviceId);

		RestClient restClient = (RestClient) ClientFactory.getNamedClient(serviceId);

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
			throw new RuntimeException(e);
		}
	}

	private Map<String, Object> debug(Verb verb, String uri,
			MultivaluedMap<String, String> headers,
			MultivaluedMap<String, String> params, InputStream requestEntity)
			throws IOException {

		Map<String, Object> info = new LinkedHashMap<String, Object>();
		if (traces != null) {

			RequestContext context = RequestContext.getCurrentContext();
			info.put("remote", true);
			info.put("serviceId", context.get("serviceId"));
			Map<String, Object> trace = new LinkedHashMap<String, Object>();
			Map<String, Object> input = new LinkedHashMap<String, Object>();
			trace.put("request", input);
			info.put("headers", trace);
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				Collection<String> collection = entry.getValue();
				Object value = collection;
				if (collection.size() < 2) {
					value = collection.isEmpty() ? "" : collection.iterator().next();
				}
				input.put(entry.getKey(), value);
			}
			StringBuilder query = new StringBuilder();
			for (String param : params.keySet()) {
				for (String value : params.get(param)) {
					query.append(param);
					query.append("=");
					query.append(value);
					query.append("&");
				}
			}

			info.put("method", verb.verb());
			info.put("uri", uri);
			info.put("query", query.toString());
			RequestContext ctx = RequestContext.getCurrentContext();
			if (!ctx.isChunkedRequestBody()) {
				if (requestEntity != null) {
					debugRequestEntity(info, ctx.getRequest().getInputStream());
				}
			}
			traces.add(info);
			return info;
		}
		return info;
	}

	private void debugRequestEntity(Map<String, Object> info, InputStream inputStream)
			throws IOException {
		String entity = IOUtils.toString(inputStream);
		if (StringUtils.hasText(entity)) {
			info.put("body", entity);
		}
	}

	private HttpResponse forward(RestClient restClient, Verb verb, String uri,
			MultivaluedMap<String, String> headers,
			MultivaluedMap<String, String> params, InputStream requestEntity)
			throws Exception {
		
		Map<String, Object> info = debug(verb, uri, headers, params, requestEntity);

		RibbonCommand command = new RibbonCommand(restClient, verb, uri, headers, params,
				requestEntity);
		try {
			HttpResponse response = command.execute();
			if (traces != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> trace = (Map<String, Object>) info.get("headers");
				Map<String, Object> output = new LinkedHashMap<String, Object>();
				trace.put("response", output);
				info.put("status", ""+response.getStatus());
				for (Entry<String, Collection<String>> key : response.getHeaders()
						.entrySet()) {
					Collection<String> collection = key.getValue();
					Object value = collection;
					if (collection.size() < 2) {
						value = collection.isEmpty() ? "" : collection.iterator().next();
					}
					output.put(key.getKey(), value);
				}
			}
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

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
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

	private MultivaluedMap<String, String> buildZuulRequestQueryParams(
			HttpServletRequest request) {

		Map<String, List<String>> map = HTTPRequestUtils.getInstance().getQueryParams();

		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		if (map == null)
			return params;

		for (String key : map.keySet()) {

			for (String value : map.get(key)) {
				params.add(key, value);
			}
		}
		return params;
	}

	private MultivaluedMap<String, String> buildZuulRequestHeaders(
			HttpServletRequest request) {

		RequestContext context = RequestContext.getCurrentContext();

		MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
		Enumeration<?> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String name = (String) headerNames.nextElement();
				String value = request.getHeader(name);
				if (!name.toLowerCase().contains("content-length"))
					headers.putSingle(name, value);
			}
		}
		Map<String, String> zuulRequestHeaders = context.getZuulRequestHeaders();

		for (String header : zuulRequestHeaders.keySet()) {
			headers.putSingle(header, zuulRequestHeaders.get(header));
		}

		headers.putSingle("accept-encoding", "deflate, gzip");

		if (headers.containsKey("transfer-encoding"))
			headers.remove("transfer-encoding");

		if (headers.containsKey("host"))
			headers.remove("host");

		return headers;
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

	void setResponse(HttpResponse resp) throws ClientException, IOException {
		RequestContext context = RequestContext.getCurrentContext();

		context.setResponseStatusCode(resp.getStatus());
		if (resp.hasEntity()) {
			context.setResponseDataStream(resp.getInputStream());
		}

		String contentEncoding = null;
		Collection<String> contentEncodingHeader = resp.getHeaders()
				.get(CONTENT_ENCODING);
		if (contentEncodingHeader != null && !contentEncodingHeader.isEmpty()) {
			contentEncoding = contentEncodingHeader.iterator().next();
		}

		if (contentEncoding != null
				&& HTTPRequestUtils.getInstance().isGzipped(contentEncoding)) {
			context.setResponseGZipped(true);
		}
		else {
			context.setResponseGZipped(false);
		}

		for (String key : resp.getHeaders().keySet()) {
			boolean isValidHeader = isValidHeader(key);
			Collection<java.lang.String> list = resp.getHeaders().get(key);
			for (String header : list) {
				context.addOriginResponseHeader(key, header);

				if (key.equalsIgnoreCase("content-length"))
					context.setOriginContentLength(header);

				if (isValidHeader) {
					context.addZuulResponseHeader(key, header);
				}
			}
		}

	}

	boolean isValidHeader(String headerName) {
		switch (headerName.toLowerCase()) {
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

}
