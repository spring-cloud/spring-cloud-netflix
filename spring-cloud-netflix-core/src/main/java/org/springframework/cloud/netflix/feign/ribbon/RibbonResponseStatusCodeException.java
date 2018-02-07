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
package org.springframework.cloud.netflix.feign.ribbon;

import feign.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException;
import org.springframework.util.StreamUtils;

/**
 * A {@link RetryableStatusCodeException} for {@link Response}s
 * @author Ryan Baxter
 */
public class RibbonResponseStatusCodeException extends RetryableStatusCodeException {
	private Response response;

	public RibbonResponseStatusCodeException(String serviceId, Response response, byte[] body, URI uri) {
		super(serviceId, response.status(), response, uri);
		this.response = Response.builder().body(new ByteArrayInputStream(body), body.length)
				.headers(response.headers()).reason(response.reason())
				.status(response.status()).request(response.request()).build();
	}

	@Override
	public Response getResponse() {
		return response;
	}

}
