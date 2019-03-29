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

package org.springframework.cloud.netflix.ribbon.apache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;

import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
public class RibbonApacheHttpRequestTests {

	@Test
	public void testNullEntity() throws Exception {
		String uri = "https://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		headers.add("content-length", "5192");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RibbonApacheHttpRequest httpRequest = new RibbonApacheHttpRequest(
				new RibbonCommandContext("example", "GET", uri, false, headers, params,
						null, new ArrayList<RibbonRequestCustomizer>()));

		HttpUriRequest request = httpRequest.toRequest(RequestConfig.custom().build());

		assertThat(request).as("request is wrong type")
				.isNotInstanceOf(HttpEntityEnclosingRequest.class);
		assertThat(request.getURI().toString()).as("uri is wrong").startsWith(uri);
		assertThat(request.getFirstHeader("my-header")).as("my-header is missing")
				.isNotNull();
		assertThat(request.getFirstHeader("my-header").getValue())
				.as("my-header is wrong").isEqualTo("my-value");
		assertThat(request.getFirstHeader("content-length").getValue())
				.as("Content-Length is wrong").isEqualTo("5192");
		assertThat(request.getURI().getQuery()).as("myparam is missing")
				.isEqualTo("myparam=myparamval");

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

	void testEntity(String entityValue, ByteArrayInputStream requestEntity,
			boolean addContentLengthHeader, String method) throws IOException {
		String lengthString = String.valueOf(entityValue.length());
		Long length = null;
		URI uri = URI.create("https://example.com");
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		if (addContentLengthHeader) {
			headers.add("Content-Length", lengthString);
			length = (long) entityValue.length();
		}

		RibbonRequestCustomizer requestCustomizer = new RibbonRequestCustomizer<RequestBuilder>() {
			@Override
			public boolean accepts(Class builderClass) {
				return builderClass == RequestBuilder.class;
			}

			@Override
			public void customize(RequestBuilder builder) {
				builder.addHeader("from-customizer", "foo");
			}
		};
		RibbonCommandContext context = new RibbonCommandContext("example", method,
				uri.toString(), false, headers, new LinkedMultiValueMap<String, String>(),
				requestEntity, Collections.singletonList(requestCustomizer));
		context.setContentLength(length);
		RibbonApacheHttpRequest httpRequest = new RibbonApacheHttpRequest(context);

		HttpUriRequest request = httpRequest.toRequest(RequestConfig.custom().build());

		assertThat(request).as("request is wrong type")
				.isInstanceOf(HttpEntityEnclosingRequest.class);
		assertThat(request.getURI().toString()).as("uri is wrong")
				.startsWith(uri.toString());
		if (addContentLengthHeader) {
			assertThat(request.getFirstHeader("Content-Length"))
					.as("Content-Length is missing").isNotNull();
			assertThat(request.getFirstHeader("Content-Length").getValue())
					.as("Content-Length is wrong").isEqualTo(lengthString);
		}
		assertThat(request.getFirstHeader("from-customizer"))
				.as("from-customizer is missing").isNotNull();
		assertThat(request.getFirstHeader("from-customizer").getValue())
				.as("from-customizer is wrong").isEqualTo("foo");

		HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
		assertThat(entityRequest.getEntity()).as("entity is missing").isNotNull();
		HttpEntity entity = entityRequest.getEntity();
		assertThat(entity.getContentLength()).as("contentLength is wrong")
				.isEqualTo((long) entityValue.length());
		assertThat(entity.getContent()).as("content is missing").isNotNull();
		String string = StreamUtils.copyToString(entity.getContent(),
				Charset.forName("UTF-8"));
		assertThat(string).as("content is wrong").isEqualTo(entityValue);
	}

}
