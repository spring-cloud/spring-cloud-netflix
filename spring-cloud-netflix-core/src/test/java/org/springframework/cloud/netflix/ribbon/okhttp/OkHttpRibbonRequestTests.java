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

package org.springframework.cloud.netflix.ribbon.okhttp;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.springframework.cloud.netflix.feign.encoding.HttpEncoding;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.util.LinkedMultiValueMap;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * @author Spencer Gibb
 */
public class OkHttpRibbonRequestTests {

	@Test
	public void testNullEntity() throws Exception {
		String uri = "http://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		// headers.add(HttpEncoding.CONTENT_LENGTH, "5192");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RibbonCommandContext context = new RibbonCommandContext("example", "GET", uri,
				false, headers, params, null, new ArrayList<RibbonRequestCustomizer>());
		OkHttpRibbonRequest httpRequest = new OkHttpRibbonRequest(context);

		Request request = httpRequest.toRequest();

		assertThat("body is not null", request.body(), is(nullValue()));
		assertThat("uri is wrong", request.url().toString(), startsWith(uri));
		assertThat("my-header is wrong", request.header("my-header"),
				is(equalTo("my-value")));
		assertThat("myparam is missing", request.url().queryParameter("myparam"),
				is(equalTo("myparamval")));
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
		String uri = "http://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		if (addContentLengthHeader) {
			headers.add("Content-Length", lengthString);
			length = (long) entityValue.length();
		}

		RibbonRequestCustomizer requestCustomizer = new RibbonRequestCustomizer<Request.Builder>() {
			@Override
			public boolean accepts(Class builderClass) {
				return builderClass == Request.Builder.class;
			}

			@Override
			public void customize(Request.Builder builder) {
				builder.addHeader("from-customizer", "foo");
			}
		};
		RibbonCommandContext context = new RibbonCommandContext("example", method, uri,
				false, headers, new LinkedMultiValueMap<String, String>(), requestEntity,
				Collections.singletonList(requestCustomizer));
		context.setContentLength(length);
		OkHttpRibbonRequest httpRequest = new OkHttpRibbonRequest(context);

		Request request = httpRequest.toRequest();

		assertThat("uri is wrong", request.url().toString(), startsWith(uri));
		if (addContentLengthHeader) {
			assertThat("Content-Length is wrong", request.header("Content-Length"),
					is(equalTo(lengthString)));
		}
		assertThat("from-customizer is wrong", request.header("from-customizer"),
				is(equalTo("foo")));

		if (!method.equalsIgnoreCase("get")) {
			assertThat("body is null", request.body(), is(notNullValue()));
			RequestBody body = request.body();
			assertThat("contentLength is wrong", body.contentLength(),
					is(equalTo((long) entityValue.length())));
			Buffer content = new Buffer();
			body.writeTo(content);
			String string = content.readByteString().utf8();
			assertThat("content is wrong", string, is(equalTo(entityValue)));
		}
	}
}
