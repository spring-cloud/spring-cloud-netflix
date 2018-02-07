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
package org.springframework.cloud.netflix.ribbon.apache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException;

/**
 * A {@link RetryableStatusCodeException} for {@link HttpResponse}s
 * @author Ryan Baxter
 */
public class HttpClientStatusCodeException extends RetryableStatusCodeException {

	private BasicHttpResponse response;

	public HttpClientStatusCodeException(String serviceId, HttpResponse response, HttpEntity entity, URI uri) throws IOException {
		super(serviceId, response.getStatusLine().getStatusCode(), response, uri);
		this.response = new BasicHttpResponse(response.getStatusLine());
		this.response.setLocale(response.getLocale());
		this.response.setStatusCode(response.getStatusLine().getStatusCode());
		this.response.setReasonPhrase(response.getStatusLine().getReasonPhrase());
		this.response.setHeaders(response.getAllHeaders());
		EntityUtils.updateEntity(this.response, entity);
	}

	@Override
	public HttpResponse getResponse() {
		return this.response;
	}
}
