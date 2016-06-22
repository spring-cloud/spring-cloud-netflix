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

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.google.common.reflect.TypeToken;
import com.netflix.client.ClientException;
import com.netflix.client.http.CaseInsensitiveMultiMap;
import com.netflix.client.http.HttpHeaders;

import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author Spencer Gibb
 */
public class OkHttpRibbonResponse implements com.netflix.client.http.HttpResponse {

	private final ResponseBody body;
	private URI uri;
	private Response response;

	public OkHttpRibbonResponse(Response response, URI uri) {
		Assert.notNull(response, "response can not be null");
		this.response = response;
		this.body = response.body();
		this.uri = uri;
	}


	@Override
	public int getStatus() {
		return this.response.code();
	}

	@Override
	public String getStatusLine() {
		return this.response.message();
	}

	@Override
	public Object getPayload() throws ClientException {
		if (!hasPayload()) {
			return null;
		}
		return this.body.byteStream();
	}

	@Override
	public boolean hasPayload() {
		return this.body != null;
	}

	@Override
	public boolean isSuccess() {
		return this.response.isSuccessful();
	}

	@Override
	public URI getRequestedURI() {
		return this.uri;
	}

	@Override
	public Map<String, Collection<String>> getHeaders() {
		final Map<String, Collection<String>> headers = new HashMap<>();
		for (Map.Entry<String,List<String>>	entry : this.response.headers().toMultimap().entrySet()) {
			String name = entry.getKey();
			for (String value : entry.getValue()) {
				if (headers.containsKey(name)) {
					headers.get(name).add(value);
				} else {
					final List<String> values = new ArrayList<>();
					values.add(value);
					headers.put(name, values);
				}
			}
		}

		return headers;
	}

	@Override
	public HttpHeaders getHttpHeaders() {
		final CaseInsensitiveMultiMap headers = new CaseInsensitiveMultiMap();
		for (Map.Entry<String,List<String>>	entry : this.response.headers().toMultimap().entrySet()) {
			for (String value : entry.getValue()) {
				headers.addHeader(entry.getKey(), value);
			}
		}

		return headers;
	}

	@Override
	public void close() {
		this.response.close();
	}

	@Override
	public InputStream getInputStream() {
		if (this.body == null) {
			return null;
		}
		return this.body.byteStream();
	}

	@Override
	public boolean hasEntity() {
		return hasPayload();
	}

	@Override
	public <T> T getEntity(Class<T> type) throws Exception {
		return null;
	}

	@Override
	public <T> T getEntity(Type type) throws Exception {
		return null;
	}

	@Override
	public <T> T getEntity(TypeToken<T> type) throws Exception {
		return null;
	}
}
