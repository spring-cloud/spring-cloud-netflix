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

package org.springframework.cloud.netflix.ribbon.okhttp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import org.junit.Test;

import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
public class OkHttpRibbonRequestTests {

	@Test
	public void testNullEntity() throws Exception {
		String uri = "https://example.com";
		LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("my-header", "my-value");
		// headers.add(HttpEncoding.CONTENT_LENGTH, "5192");
		LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("myparam", "myparamval");
		RibbonCommandContext context = new RibbonCommandContext("example", "GET", uri,
				false, headers, params, null, new ArrayList<RibbonRequestCustomizer>());
		OkHttpRibbonRequest httpRequest = new OkHttpRibbonRequest(context);

		Request request = httpRequest.toRequest();

		assertThat(request.body()).as("body is not null").isNull();
		assertThat(request.url().toString()).as("uri is wrong").startsWith(uri);
		assertThat(request.header("my-header")).as("my-header is wrong")
				.isEqualTo("my-value");
		assertThat(request.url().queryParameter("myparam")).as("myparam is missing")
				.isEqualTo("myparamval");
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
		String uri = "https://example.com";
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

		assertThat(request.url().toString()).as("uri is wrong").startsWith(uri);
		if (addContentLengthHeader) {
			assertThat(request.header("Content-Length")).as("Content-Length is wrong")
					.isEqualTo(lengthString);
		}
		assertThat(request.header("from-customizer")).as("from-customizer is wrong")
				.isEqualTo("foo");

		if (!method.equalsIgnoreCase("get")) {
			assertThat(request.body()).as("body is null").isNotNull();
			RequestBody body = request.body();
			assertThat(body.contentLength()).as("contentLength is wrong")
					.isEqualTo((long) entityValue.length());
			Buffer content = new Buffer();
			body.writeTo(content);
			String string = content.readByteString().utf8();
			assertThat(string).as("content is wrong").isEqualTo(entityValue);
		}
	}

}
