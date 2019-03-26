/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpRequest.Verb;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
public class RestClientRibbonCommandTests {

	private ZuulProperties zuulProperties;

	@Before
	public void setUp() {
		zuulProperties = new ZuulProperties();
	}

	/**
	 * Tests old constructors kept for backwards compatibility with Spring Cloud Sleuth
	 * 1.x versions
	 */
	@Test
	@Deprecated
	public void testNullEntityWithOldConstruct() throws Exception {
		String uri = "https://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RestClientRibbonCommand command = new RestClientRibbonCommand("cmd", null,
				Verb.GET, uri, false, headers, params, null);

		HttpRequest request = command.createRequest();

		assertThat(request.getUri().toString()).as("uri is wrong").startsWith(uri);
		assertThat(request.getHttpHeaders().getFirstValue("my-header"))
				.as("my-header is wrong").isEqualTo("my-value");
		assertThat(request.getQueryParams().get("myparam").iterator().next())
				.as("myparam is missing").isEqualTo("myparamval");

		command = new RestClientRibbonCommand("cmd", null, new RibbonCommandContext(
				"example", "GET", uri, false, headers, params, null), zuulProperties);

		request = command.createRequest();

		assertThat(request.getUri().toString()).as("uri is wrong").startsWith(uri);
		assertThat(request.getHttpHeaders().getFirstValue("my-header"))
				.as("my-header is wrong").isEqualTo("my-value");
		assertThat(request.getQueryParams().get("myparam").iterator().next())
				.as("myparam is missing").isEqualTo("myparamval");
	}

	@Test
	public void testNullEntity() throws Exception {
		String uri = "https://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RestClientRibbonCommand command = new RestClientRibbonCommand(
				"cmd", null, new RibbonCommandContext("example", "GET", uri, false,
						headers, params, null, new ArrayList<RibbonRequestCustomizer>()),
				zuulProperties);

		HttpRequest request = command.createRequest();

		assertThat(request.getUri().toString()).as("uri is wrong").startsWith(uri);
		assertThat(request.getHttpHeaders().getFirstValue("my-header"))
				.as("my-header is wrong").isEqualTo("my-value");
		assertThat(request.getQueryParams().get("myparam").iterator().next())
				.as("myparam is missing").isEqualTo("myparamval");
	}

	@Test
	// this situation happens, see
	// https://github.com/spring-cloud/spring-cloud-netflix/issues/1042#issuecomment-227723877
	public void testEmptyEntityGet() throws Exception {
		String entityValue = "";
		testEntity(entityValue, new ByteArrayInputStream(entityValue.getBytes()), false,
				"GET");
	}

	@Test
	public void testNonEmptyEntityPost() throws Exception {
		String entityValue = "abcd";
		testEntity(entityValue, new ByteArrayInputStream(entityValue.getBytes()), true,
				"POST");
	}

	@Test
	public void testNonEmptyEntityDelete() throws Exception {
		String entityValue = "abcd";
		testEntity(entityValue, new ByteArrayInputStream(entityValue.getBytes()), true,
				"DELETE");
	}

	void testEntity(String entityValue, ByteArrayInputStream requestEntity,
			boolean addContentLengthHeader, String method) throws Exception {
		String lengthString = String.valueOf(entityValue.length());
		Long length = null;
		URI uri = URI.create("https://example.com");
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		if (addContentLengthHeader) {
			headers.add("Content-Length", lengthString);
			length = (long) entityValue.length();
		}

		RibbonRequestCustomizer requestCustomizer = new RibbonRequestCustomizer<HttpRequest.Builder>() {
			@Override
			public boolean accepts(Class builderClass) {
				return builderClass == HttpRequest.Builder.class;
			}

			@Override
			public void customize(HttpRequest.Builder builder) {
				builder.header("from-customizer", "foo");
			}
		};
		RibbonCommandContext context = new RibbonCommandContext("example", method,
				uri.toString(), false, headers, new LinkedMultiValueMap<String, String>(),
				requestEntity, Collections.singletonList(requestCustomizer));
		context.setContentLength(length);
		RestClientRibbonCommand command = new RestClientRibbonCommand("cmd", null,
				context, zuulProperties);

		HttpRequest request = command.createRequest();

		assertThat(request.getUri().toString()).as("uri is wrong")
				.startsWith(uri.toString());
		if (addContentLengthHeader) {
			assertThat(request.getHttpHeaders().getFirstValue("Content-Length"))
					.as("Content-Length is wrong").isEqualTo(lengthString);
		}
		assertThat(request.getHttpHeaders().getFirstValue("from-customizer"))
				.as("from-customizer is wrong").isEqualTo("foo");

		if (method.equalsIgnoreCase("DELETE")) {
			assertThat(request.getEntity()).as("entity is was non-null").isNull();
		}
		else {
			assertThat(request.getEntity()).as("entity is missing").isNotNull();
			assertThat(InputStream.class.isAssignableFrom(request.getEntity().getClass()))
					.as("entity is wrong type").isTrue();
			InputStream entity = (InputStream) request.getEntity();
			String string = StreamUtils.copyToString(entity, Charset.forName("UTF-8"));
			assertThat(string).as("content is wrong").isEqualTo(entityValue);
		}
	}

}
