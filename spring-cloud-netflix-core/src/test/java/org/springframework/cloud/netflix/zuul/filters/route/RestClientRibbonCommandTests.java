/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StreamUtils;

import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpRequest.Verb;

/**
 * @author Spencer Gibb
 */
public class RestClientRibbonCommandTests {

	private ZuulProperties zuulProperties;
	
	@Before
	public void setUp()	{
		zuulProperties = new ZuulProperties();
	}
	
	/**
	 * Tests old constructors kept for backwards compatibility with Spring Cloud Sleuth 1.x versions
	 */
	@Test
	@Deprecated
	public void testNullEntityWithOldConstruct() throws Exception {
		String uri = "http://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RestClientRibbonCommand command = 
				new RestClientRibbonCommand("cmd", null,Verb.GET ,uri, false, headers, params, null);

		HttpRequest request = command.createRequest();

		assertThat("uri is wrong", request.getUri().toString(), startsWith(uri));
		assertThat("my-header is wrong", request.getHttpHeaders().getFirstValue("my-header"), is(equalTo("my-value")));
		assertThat("myparam is missing", request.getQueryParams().get("myparam").iterator().next(), is(equalTo("myparamval")));
		
		command = 
				new RestClientRibbonCommand("cmd", null,
				new RibbonCommandContext("example", "GET", uri, false, headers, params, null),
				zuulProperties);

		request = command.createRequest();

		assertThat("uri is wrong", request.getUri().toString(), startsWith(uri));
		assertThat("my-header is wrong", request.getHttpHeaders().getFirstValue("my-header"), is(equalTo("my-value")));
		assertThat("myparam is missing", request.getQueryParams().get("myparam").iterator().next(), is(equalTo("myparamval")));
	}
	
	@Test
	public void testNullEntity() throws Exception {
		String uri = "http://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RestClientRibbonCommand command = 
				new RestClientRibbonCommand("cmd", null,
				new RibbonCommandContext("example", "GET", uri, false, headers, params, null, new ArrayList<RibbonRequestCustomizer>()),
				zuulProperties);

		HttpRequest request = command.createRequest();

		assertThat("uri is wrong", request.getUri().toString(), startsWith(uri));
		assertThat("my-header is wrong", request.getHttpHeaders().getFirstValue("my-header"), is(equalTo("my-value")));
		assertThat("myparam is missing", request.getQueryParams().get("myparam").iterator().next(), is(equalTo("myparamval")));
	}

	@Test
	// this situation happens, see https://github.com/spring-cloud/spring-cloud-netflix/issues/1042#issuecomment-227723877
	public void testEmptyEntityGet() throws Exception {
		String entityValue = "";
		testEntity(entityValue, new ByteArrayInputStream(entityValue.getBytes()), false, "GET");
	}

	@Test
	public void testNonEmptyEntityPost() throws Exception {
		String entityValue = "abcd";
		testEntity(entityValue, new ByteArrayInputStream(entityValue.getBytes()), true, "POST");
	}

	@Test
	public void testNonEmptyEntityDelete() throws Exception {
		String entityValue = "abcd";
		testEntity(entityValue, new ByteArrayInputStream(entityValue.getBytes()), true, "DELETE");
	}

	void testEntity(String entityValue, ByteArrayInputStream requestEntity, boolean addContentLengthHeader, String method) throws Exception {
		String lengthString = String.valueOf(entityValue.length());
		Long length = null;
		URI uri = URI.create("http://example.com");
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
		RestClientRibbonCommand command = new RestClientRibbonCommand("cmd", null, context, zuulProperties);

		HttpRequest request = command.createRequest();

		assertThat("uri is wrong", request.getUri().toString(), startsWith(uri.toString()));
		if (addContentLengthHeader) {
			assertThat("Content-Length is wrong", request.getHttpHeaders().getFirstValue("Content-Length"),
					is(equalTo(lengthString)));
		}
		assertThat("from-customizer is wrong", request.getHttpHeaders().getFirstValue("from-customizer"),
				is(equalTo("foo")));


		if (method.equalsIgnoreCase("DELETE")) {
			assertThat("entity is was non-null", request.getEntity(), is(nullValue()));
		} else {
			assertThat("entity is missing", request.getEntity(), is(notNullValue()));
			assertThat("entity is wrong type", InputStream.class.isAssignableFrom(request.getEntity().getClass()), is(true));
			InputStream entity = (InputStream) request.getEntity();
			String string = StreamUtils.copyToString(entity, Charset.forName("UTF-8"));
			assertThat("content is wrong", string, is(equalTo(entityValue)));
		}
	}
}
