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

import java.net.URI;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
public class OkHttpRibbonResponseTests {

	@Test
	public void testNullEntity() throws Exception {
		URI uri = URI.create("https://example.com");
		Response response = response(uri).build();

		OkHttpRibbonResponse httpResponse = new OkHttpRibbonResponse(response, uri);

		assertThat(httpResponse.isSuccess()).isTrue();
		assertThat(httpResponse.hasPayload()).isFalse();
		assertThat(httpResponse.getPayload()).isNull();
		assertThat(httpResponse.getInputStream()).isNull();
	}

	@Test
	public void testNotNullEntity() throws Exception {
		URI uri = URI.create("https://example.com");
		Response response = response(uri)
				.body(ResponseBody.create(MediaType.parse("text/plain"), "abcd")).build();

		OkHttpRibbonResponse httpResponse = new OkHttpRibbonResponse(response, uri);

		assertThat(httpResponse.isSuccess()).isTrue();
		assertThat(httpResponse.hasPayload()).isTrue();
		assertThat(httpResponse.getPayload()).isNotNull();
		assertThat(httpResponse.getInputStream()).isNotNull();
	}

	Response.Builder response(URI uri) {
		return new Response.Builder()
				.request(new Request.Builder().url(HttpUrl.get(uri)).build())
				.protocol(Protocol.HTTP_1_1).code(HttpStatus.OK.value())
				.message(HttpStatus.OK.getReasonPhrase());
	}

}
