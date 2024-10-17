/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Response that ignores body, specifically for 404 errors.
 *
 * @author Spencer Gibb
 * @author Wonchul Heo
 * @since 4.2.0
 */
class NotFoundHttpResponse implements ClientHttpResponse {

	private final ClientHttpResponse response;

	NotFoundHttpResponse(ClientHttpResponse response) {
		this.response = response;
	}

	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return response.getStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return response.getStatusText();
	}

	@Override
	public void close() {
		response.close();
	}

	@Override
	public InputStream getBody() throws IOException {
		// ignore body on 404 for heartbeat, see gh-4145
		return null;
	}

	@Override
	public HttpHeaders getHeaders() {
		return response.getHeaders();
	}

}
