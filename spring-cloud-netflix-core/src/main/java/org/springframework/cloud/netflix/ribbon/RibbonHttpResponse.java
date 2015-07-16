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

package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.http.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Spencer Gibb
 */
public class RibbonHttpResponse extends AbstractClientHttpResponse {

	private HttpResponse response;
	private HttpHeaders httpHeaders;

	public RibbonHttpResponse(HttpResponse response) {
		this.response = response;
		this.httpHeaders = new HttpHeaders();
		List<Map.Entry<String, String>> headers = response.getHttpHeaders()
				.getAllHeaders();
		for (Map.Entry<String, String> header : headers) {
			this.httpHeaders.add(header.getKey(), header.getValue());
		}
	}

	@Override
	public InputStream getBody() throws IOException {
		return response.getInputStream();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.httpHeaders;
	}

	@Override
	public int getRawStatusCode() throws IOException {
		return response.getStatus();
	}

	@Override
	public String getStatusText() throws IOException {
		return HttpStatus.valueOf(response.getStatus()).name();
	}

	@Override
	public void close() {
		response.close();
	}

}
