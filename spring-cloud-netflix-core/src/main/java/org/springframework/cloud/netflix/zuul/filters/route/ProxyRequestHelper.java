/*
 * Copyright 2013-2014 the original author or authors.
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
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.util.StringUtils;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author Dave Syer
 *
 */
public class ProxyRequestHelper {

	public static final String CONTENT_ENCODING = "Content-Encoding";

	private TraceRepository traces;

	public void setTraces(TraceRepository traces) {
		this.traces = traces;
	}

	public MultivaluedMap<String, String> buildZuulRequestQueryParams(
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

	public MultivaluedMap<String, String> buildZuulRequestHeaders(
			HttpServletRequest request) {

		RequestContext context = RequestContext.getCurrentContext();

		MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
		Enumeration<?> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String name = (String) headerNames.nextElement();
				String value = request.getHeader(name);
				if (isIncludedHeader(name))
					headers.putSingle(name, value);
			}
		}
		Map<String, String> zuulRequestHeaders = context.getZuulRequestHeaders();

		for (String header : zuulRequestHeaders.keySet()) {
			headers.putSingle(header, zuulRequestHeaders.get(header));
		}

		headers.putSingle("accept-encoding", "deflate, gzip");

		return headers;
	}

	public void setResponse(int status, InputStream entity,
			Map<String, Collection<String>> headers) throws IOException {
		RequestContext context = RequestContext.getCurrentContext();

		RequestContext.getCurrentContext().setResponseStatusCode(status);
		if (entity != null) {
			RequestContext.getCurrentContext().setResponseDataStream(entity);
		}

		boolean isOriginResponseGzipped = false;

		if (headers.containsKey(CONTENT_ENCODING)) {
			Collection<String> collection = headers.get(CONTENT_ENCODING);
			for (String header : collection) {
				if (HTTPRequestUtils.getInstance().isGzipped(header)) {
					isOriginResponseGzipped = true;
					break;
				}
			}
		}
		context.setResponseGZipped(isOriginResponseGzipped);

		for (Entry<String, Collection<String>> header : headers.entrySet()) {
			RequestContext ctx = RequestContext.getCurrentContext();
			String name = header.getKey();
			for (String value : header.getValue()) {
				ctx.addOriginResponseHeader(name, value);

				if (name.equalsIgnoreCase("content-length"))
					ctx.setOriginContentLength(value);

				if (isIncludedHeader(name)) {
					ctx.addZuulResponseHeader(name, value);
				}
			}
		}

	}

	public boolean isIncludedHeader(String headerName) {
		switch (headerName.toLowerCase()) {
		case "host":
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

	public Map<String, Object> debug(String verb, String uri,
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

			info.put("method", verb);
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

	public void appendDebug(Map<String, Object> info, int status,
			Map<String, Collection<String>> headers) {
		if (traces != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> trace = (Map<String, Object>) info.get("headers");
			Map<String, Object> output = new LinkedHashMap<String, Object>();
			trace.put("response", output);
			info.put("status", "" + status);
			for (Entry<String, Collection<String>> key : headers.entrySet()) {
				Collection<String> collection = key.getValue();
				Object value = collection;
				if (collection.size() < 2) {
					value = collection.isEmpty() ? "" : collection.iterator().next();
				}
				output.put(key.getKey(), value);
			}
		}
	}

	public void appendDebug(Map<String, Object> info, int status,
			MultivaluedMap<String, String> headers) {
		if (traces != null) {
			Map<String, Collection<String>> map = new LinkedHashMap<String, Collection<String>>();
			for (Entry<String, List<String>> key : headers.entrySet()) {
				Collection<String> collection = key.getValue();
				map.put(key.getKey(), collection);
			}
			appendDebug(info, status, map);
		}
	}

	private void debugRequestEntity(Map<String, Object> info, InputStream inputStream)
			throws IOException {
		String entity = IOUtils.toString(inputStream);
		if (StringUtils.hasText(entity)) {
			info.put("body", entity);
		}
	}

}