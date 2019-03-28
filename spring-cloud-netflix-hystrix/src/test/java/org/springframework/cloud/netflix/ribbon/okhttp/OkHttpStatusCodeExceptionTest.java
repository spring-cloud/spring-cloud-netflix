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

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class OkHttpStatusCodeExceptionTest {

	@Test
	public void getResponse() throws Exception {
		Headers headers = new Headers.Builder().add("foo", "bar").build();
		Response response = new Response.Builder().code(200).headers(headers).code(200)
				.message("Success")
				.body(ResponseBody.create(MediaType.parse("text/plain"), "foo"))
				.protocol(Protocol.HTTP_1_1)
				.request(new Request.Builder().url("https://service.com").build())
				.build();
		ResponseBody body = response.peekBody(Integer.MAX_VALUE);
		OkHttpStatusCodeException ex = new OkHttpStatusCodeException("service", response,
				body, new URI("https://service.com"));
		assertThat(ex.getResponse().headers()).isEqualTo(headers);
		assertThat(ex.getResponse().code()).isEqualTo(200);
		assertThat(ex.getResponse().message()).isEqualTo("Success");
		assertThat(ex.getResponse().body().string()).isEqualTo("foo");
		assertThat(ex.getResponse().protocol()).isEqualTo(Protocol.HTTP_1_1);
	}

}
