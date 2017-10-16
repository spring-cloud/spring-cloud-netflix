/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.cloud.netflix.eureka.server.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.jayway.jsonpath.JsonPath;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;

import org.springframework.util.Base64Utils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class RequestVerifierFilter implements Filter {

	static final String CONTEXT_KEY_CONFIGURATION = "org.springframework.restdocs.configuration";
	private Map<String, JsonPath> jsonPaths = new LinkedHashMap<>();
	private MappingBuilder builder;

	public static RequestVerifierFilter verify(String path) {
		return new RequestVerifierFilter(path);
	}

	public static RequestVerifierFilter verify(MappingBuilder builder) {
		return new RequestVerifierFilter().wiremock(builder);
	}

	private RequestVerifierFilter(String expression, Object... args) {
		expression = String.format(expression, args);
		this.jsonPaths.put(expression, JsonPath.compile(expression));
	}

	private RequestVerifierFilter() {
	}

	public RequestVerifierFilter json(String expression, Object... args) {
		expression = String.format(expression, args);
		this.jsonPaths.put(expression, JsonPath.compile(expression));
		return this;
	}

	public RequestVerifierFilter wiremock(MappingBuilder builder) {
		this.builder = builder;
		return this;
	}

	@Override
	public Response filter(FilterableRequestSpecification requestSpec,
			FilterableResponseSpecification responseSpec, FilterContext context) {
		Map<String, Object> configuration = getConfiguration(requestSpec, context);
		configuration.put("contract.jsonPaths", this.jsonPaths.keySet());
		Response response = context.next(requestSpec, responseSpec);
		if (requestSpec.getBody() != null && !this.jsonPaths.isEmpty()) {
			String actual = new String((byte[]) requestSpec.getBody());
			for (JsonPath jsonPath : this.jsonPaths.values()) {
				new JsonPathValue(jsonPath, actual).assertHasValue(Object.class,
						"an object");
			}
		}
		if (this.builder != null) {
			this.builder.willReturn(getResponseDefinition(response));
			StubMapping stubMapping = this.builder.build();
			MatchResult match = stubMapping.getRequest()
					.match(new WireMockRestAssuredRequestAdapter(requestSpec));
			assertThat(match.isExactMatch()).as("wiremock did not match request")
					.isTrue();
			configuration.put("contract.stubMapping", stubMapping);
		}
		return response;
	}

	private ResponseDefinitionBuilder getResponseDefinition(Response response) {
		ResponseDefinitionBuilder definition = ResponseDefinitionBuilder
				.responseDefinition().withBody(response.getBody().asString())
				.withStatus(response.getStatusCode());
		addResponseHeaders(definition, response);
		return definition;
	}

	private void addResponseHeaders(ResponseDefinitionBuilder definition,
			Response input) {
		for (Header header : input.getHeaders().asList()) {
			String name = header.getName();
			definition.withHeader(name, input.getHeader(name));
		}
	}

	protected Map<String, Object> getConfiguration(
			FilterableRequestSpecification requestSpec, FilterContext context) {
		Map<String, Object> configuration = context
				.<Map<String, Object>>getValue(CONTEXT_KEY_CONFIGURATION);
		return configuration;
	}
}

class JsonPathValue {

	private final JsonPath jsonPath;
	private final String expression;
	private final CharSequence actual;

	JsonPathValue(JsonPath jsonPath, CharSequence actual) {
		this.jsonPath = jsonPath;
		this.actual = actual;
		this.expression = jsonPath.getPath();
	}

	public void assertHasValue(Class<?> type, String expectedDescription) {
		Object value = getValue(true);
		if (value == null || isIndefiniteAndEmpty()) {
			throw new AssertionError(getNoValueMessage());
		}
		if (type != null && !type.isInstance(value)) {
			throw new AssertionError(getExpectedValueMessage(expectedDescription));
		}
	}

	private boolean isIndefiniteAndEmpty() {
		return !isDefinite() && isEmpty();
	}

	private boolean isDefinite() {
		return this.jsonPath.isDefinite();
	}

	private boolean isEmpty() {
		return ObjectUtils.isEmpty(getValue(false));
	}

	public Object getValue(boolean required) {
		try {
			CharSequence json = this.actual;
			return this.jsonPath.read(json == null ? null : json.toString());
		}
		catch (Exception ex) {
			if (!required) {
				return null;
			}
			throw new AssertionError(getNoValueMessage() + ". " + ex.getMessage());
		}
	}

	private String getNoValueMessage() {
		return "No value at JSON path \"" + this.expression + "\"";
	}

	private String getExpectedValueMessage(String expectedDescription) {
		return String.format("Expected %s at JSON path \"%s\" but found: %s",
				expectedDescription, this.expression,
				ObjectUtils.nullSafeToString(StringUtils.quoteIfString(getValue(false))));
	}

}

class WireMockRestAssuredRequestAdapter implements Request {

	private FilterableRequestSpecification request;

	public WireMockRestAssuredRequestAdapter(FilterableRequestSpecification request) {
		this.request = request;
	}

	@Override
	public String getUrl() {
		return request.getDerivedPath();
	}

	@Override
	public String getAbsoluteUrl() {
		return request.getURI();
	}

	@Override
	public RequestMethod getMethod() {
		return RequestMethod.fromString(request.getMethod().name());
	}

	@Override
	public String getClientIp() {
		return "127.0.0.1";
	}

	@Override
	public String getHeader(String key) {
		String value = request.getHeaders().getValue(key);
		if ("accept".equals(key.toLowerCase()) && "*/*".equals(value)) {
			return null;
		}
		return value;
	}

	@Override
	public HttpHeader header(String key) {
		String value = request.getHeaders().getValue(key);
		if ("accept".equals(key.toLowerCase()) && "*/*".equals(value)) {
			return null;
		}
		return new HttpHeader(key, value);
	}

	@Override
	public ContentTypeHeader contentTypeHeader() {
		return new ContentTypeHeader(request.getContentType());
	}

	@Override
	public HttpHeaders getHeaders() {
		List<HttpHeader> headers = new ArrayList<>();
		for (Header header : request.getHeaders()) {
			String value = header.getValue();
			if ("accept".equals(header.getName().toLowerCase()) && "*/*".equals(value)) {
				continue;
			}
			headers.add(new HttpHeader(header.getName(), header.getValue()));
		}
		return new HttpHeaders(headers);
	}

	@Override
	public boolean containsHeader(String key) {
		String value = request.getHeaders().getValue(key);
		if ("accept".equals(key.toLowerCase()) && "*/*".equals(value)) {
			return false;
		}
		return request.getHeaders().hasHeaderWithName(key);
	}

	@Override
	public Set<String> getAllHeaderKeys() {
		Set<String> headers = new LinkedHashSet<>();
		for (Header header : request.getHeaders()) {
			String value = header.getValue();
			if ("accept".equals(header.getName().toLowerCase()) && "*/*".equals(value)) {
				continue;
			}
			headers.add(header.getName());
		}
		return headers;
	}

	@Override
	public Map<String, Cookie> getCookies() {
		Map<String, Cookie> map = new LinkedHashMap<>();
		for (com.jayway.restassured.response.Cookie cookie : request.getCookies()) {
			Cookie value = new Cookie(cookie.getValue());
			map.put(cookie.getName(), value);
		}
		return map;
	}

	@Override
	public QueryParameter queryParameter(String key) {
		Map<String, String> params = request.getQueryParams();
		if (params.containsKey(key)) {
			return new QueryParameter(key, Arrays.asList(params.get(key)));
		}
		return null;
	}

	@Override
	public byte[] getBody() {
		return request.getBody();
	}

	@Override
	public String getBodyAsString() {
		return new String(getBody());
	}

	@Override
	public String getBodyAsBase64() {
		return Base64Utils.encodeToString(getBody());
	}

	@Override
	public boolean isBrowserProxyRequest() {
		return false;
	}

}
