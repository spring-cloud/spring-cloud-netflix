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

package org.springframework.cloud.netflix.ribbon.apache;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

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
import org.springframework.cloud.netflix.feign.encoding.HttpEncoding;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StreamUtils;

/**
 * @author Spencer Gibb
 */
public class RibbonApacheHttpRequestTests {

	@Test
	public void testNullEntity() throws Exception {
		String uri = "http://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		headers.add(HttpEncoding.CONTENT_LENGTH, "5192");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RibbonApacheHttpRequest httpRequest = 
				new RibbonApacheHttpRequest(
						new RibbonCommandContext("example", "GET", uri, false, headers, params, null, new ArrayList<RibbonRequestCustomizer>()));

		HttpUriRequest request = httpRequest.toRequest(RequestConfig.custom().build());

		assertThat("request is wrong type", request, is(not(instanceOf(HttpEntityEnclosingRequest.class))));
		assertThat("uri is wrong", request.getURI().toString(), startsWith(uri));
		assertThat("my-header is missing", request.getFirstHeader("my-header"), is(notNullValue()));
		assertThat("my-header is wrong", request.getFirstHeader("my-header").getValue(), is(equalTo("my-value")));
		assertThat("Content-Length is wrong", request.getFirstHeader(HttpEncoding.CONTENT_LENGTH).getValue(), is(equalTo("5192")));
		assertThat("myparam is missing", request.getURI().getQuery(), is(equalTo("myparam=myparamval")));
		
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

	void testEntity(String entityValue, ByteArrayInputStream requestEntity, boolean addContentLengthHeader, String method) throws IOException {
		String lengthString = String.valueOf(entityValue.length());
		Long length = null;
		URI uri = URI.create("http://example.com");
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

		assertThat("request is wrong type", request, is(instanceOf(HttpEntityEnclosingRequest.class)));
		assertThat("uri is wrong", request.getURI().toString(), startsWith(uri.toString()));
		if (addContentLengthHeader) {
			assertThat("Content-Length is missing", request.getFirstHeader("Content-Length"), is(notNullValue()));
			assertThat("Content-Length is wrong", request.getFirstHeader("Content-Length").getValue(),
					is(equalTo(lengthString)));
		}
		assertThat("from-customizer is missing", request.getFirstHeader("from-customizer"), is(notNullValue()));
		assertThat("from-customizer is wrong", request.getFirstHeader("from-customizer").getValue(),
				is(equalTo("foo")));

		HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
		assertThat("entity is missing", entityRequest.getEntity(), is(notNullValue()));
		HttpEntity entity = entityRequest.getEntity();
		assertThat("contentLength is wrong", entity.getContentLength(), is(equalTo((long)entityValue.length())));
		assertThat("content is missing", entity.getContent(), is(notNullValue()));
		String string = StreamUtils.copyToString(entity.getContent(), Charset.forName("UTF-8"));
		assertThat("content is wrong", string, is(equalTo(entityValue)));
	}
}

