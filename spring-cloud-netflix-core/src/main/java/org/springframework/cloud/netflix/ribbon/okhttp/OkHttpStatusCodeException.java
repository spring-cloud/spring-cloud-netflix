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

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URI;
import org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException;

/**
 * An {@link RetryableStatusCodeException} that captures a {@link Response}
 * @author Ryan Baxter
 */
public class OkHttpStatusCodeException extends RetryableStatusCodeException {
	private Response response;

	public OkHttpStatusCodeException(String serviceId, Response response, ResponseBody responseBody, URI uri) {
		super(serviceId, response.code(), response, uri);
		this.response = new Response.Builder().code(response.code()).message(response.message()).protocol(response.protocol())
				.request(response.request()).headers(response.headers()).handshake(response.handshake())
				.cacheResponse(response.cacheResponse()).networkResponse(response.networkResponse())
				.priorResponse(response.priorResponse()).sentRequestAtMillis(response.sentRequestAtMillis())
				.body(responseBody).build();
	}

	@Override
	public Response getResponse() {
		return response;
	}
}
