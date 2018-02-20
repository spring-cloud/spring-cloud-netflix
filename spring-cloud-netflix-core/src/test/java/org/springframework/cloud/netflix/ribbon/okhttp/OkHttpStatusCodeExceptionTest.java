/*
 * Copyright 2013-2018 the original author or authors.
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

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.URI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;


/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class OkHttpStatusCodeExceptionTest {

	@Test
	public void getResponse() throws Exception {
		Headers headers = new Headers.Builder().add("foo", "bar").build();
		Response response = new Response.Builder().code(200).headers(headers).code(200).message("Success")
				.body(ResponseBody.create(MediaType.parse("text/plain"), "foo")).protocol(Protocol.HTTP_1_1)
				.request(new Request.Builder().url("http://service.com").build()).build();
		ResponseBody body = response.peekBody(Integer.MAX_VALUE);
		OkHttpStatusCodeException ex = new OkHttpStatusCodeException("service", response, body, new URI("http://service.com"));
		assertEquals(headers, ex.getResponse().headers());
		assertEquals(200, ex.getResponse().code());
		assertEquals("Success", ex.getResponse().message());
		assertEquals("foo", ex.getResponse().body().string());
		assertEquals(Protocol.HTTP_1_1, ex.getResponse().protocol());
	}
}