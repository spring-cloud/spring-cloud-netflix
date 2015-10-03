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

package org.springframework.cloud.netflix.ribbon.apache;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.netflix.client.ClientException;
import com.netflix.client.http.CaseInsensitiveMultiMap;
import com.netflix.client.http.HttpHeaders;

/**
 * @author Christian Lohmann
 */
public class RibbonApacheHttpResponse implements com.netflix.client.http.HttpResponse {

	private HttpResponse httpResponse;
	private URI uri;

	public RibbonApacheHttpResponse(final HttpResponse httpResponse, final URI uri) {
		this.httpResponse = httpResponse;
		this.uri = uri;
	}

	@Override
	public Object getPayload() throws ClientException {
		try {
			return this.httpResponse.getEntity().getContent();
		}
		catch (final IOException e) {
			throw new ClientException(e.getMessage(), e);
		}
	}

	@Override
	public boolean hasPayload() {
		return this.httpResponse.getEntity() != null;
	}

	@Override
	public boolean isSuccess() {
		return this.httpResponse.getStatusLine().getStatusCode() == 200;
	}

	@Override
	public URI getRequestedURI() {
		return this.uri;
	}

	@Override
	public int getStatus() {
		return httpResponse.getStatusLine().getStatusCode();
	}

	@Override
	public String getStatusLine() {
		return httpResponse.getStatusLine().toString();
	}

	@Override
	public Map<String, Collection<String>> getHeaders() {
		final Map<String, Collection<String>> headers = Maps.newHashMap();
		for (final Header header : this.httpResponse.getAllHeaders()) {
			if (headers.containsKey(header.getName())) {
				headers.get(header.getName()).add(header.getValue());
			}
			else {
				headers.put(header.getName(), Lists.newArrayList(header.getValue()));
			}
		}

		return headers;
	}

	@Override
	public HttpHeaders getHttpHeaders() {
		final CaseInsensitiveMultiMap headers = new CaseInsensitiveMultiMap();
		for (final Header header : httpResponse.getAllHeaders()) {
			headers.addHeader(header.getName(), header.getValue());
		}

		return headers;
	}

	@Override
	public void close() {
		if (this.httpResponse != null && this.httpResponse.getEntity() != null) {
			try {
				this.httpResponse.getEntity().getContent().close();
			}
			catch (final IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

	}

	@Override
	public InputStream getInputStream() {
		try {
			return this.httpResponse.getEntity().getContent();
		}
		catch (final IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public boolean hasEntity() {
		return hasPayload();
	}

	@Override
	public <T> T getEntity(final Class<T> type) throws Exception {
		return null;
	}

	@Override
	public <T> T getEntity(final Type type) throws Exception {
		return null;
	}

	@Override
	public <T> T getEntity(final TypeToken<T> type) throws Exception {
		return null;
	}
}
