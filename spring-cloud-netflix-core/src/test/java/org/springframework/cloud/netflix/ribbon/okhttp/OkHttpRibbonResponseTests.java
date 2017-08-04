/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon.okhttp;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.junit.Test;
import org.springframework.http.HttpStatus;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author Spencer Gibb
 */
public class OkHttpRibbonResponseTests {

	@Test
	public void testNullEntity() throws Exception {
		URI uri = URI.create("http://example.com");
		Response response = response(uri).build();

		OkHttpRibbonResponse httpResponse = new OkHttpRibbonResponse(response, uri);

		assertThat(httpResponse.isSuccess(), is(true));
		assertThat(httpResponse.hasPayload(), is(false));
		assertThat(httpResponse.getPayload(), is(nullValue()));
		assertThat(httpResponse.getInputStream(), is(nullValue()));
	}

	@Test
	public void testNotNullEntity() throws Exception {
		URI uri = URI.create("http://example.com");
		Response response = response(uri)
				.body(ResponseBody.create(MediaType.parse("text/plain"), "abcd"))
				.build();

		OkHttpRibbonResponse httpResponse = new OkHttpRibbonResponse(response, uri);

		assertThat(httpResponse.isSuccess(), is(true));
		assertThat(httpResponse.hasPayload(), is(true));
		assertThat(httpResponse.getPayload(), is(notNullValue()));
		assertThat(httpResponse.getInputStream(), is(notNullValue()));
	}

	Response.Builder response(URI uri) {
		return new Response.Builder()
				.request(new Request.Builder().url(HttpUrl.get(uri)).build())
				.protocol(Protocol.HTTP_1_1)
				.code(HttpStatus.OK.value())
				.message(HttpStatus.OK.getReasonPhrase());
	}
}
