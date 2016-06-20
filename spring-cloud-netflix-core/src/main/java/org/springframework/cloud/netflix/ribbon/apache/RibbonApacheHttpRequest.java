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

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.springframework.util.MultiValueMap;

import com.netflix.client.ClientRequest;

import lombok.Getter;

/**
 * @author Christian Lohmann
 */
@Getter
public class RibbonApacheHttpRequest extends ClientRequest implements Cloneable {

	private final String method;
	private Long contentLength;

	private final MultiValueMap<String, String> headers;

	private final MultiValueMap<String, String> params;

	private final InputStream requestEntity;

	public RibbonApacheHttpRequest(final String method, final URI uri,
								   final Boolean retryable, final MultiValueMap<String, String> headers,
								   final MultiValueMap<String, String> params, final InputStream requestEntity) {
		this(method, uri, retryable, headers, params, requestEntity, null);
	}

	public RibbonApacheHttpRequest(final String method, final URI uri,
			final Boolean retryable, final MultiValueMap<String, String> headers,
			final MultiValueMap<String, String> params, final InputStream requestEntity, Long contentLength) {

		this.method = method;
		this.contentLength = contentLength;
		this.uri = uri;
		this.isRetriable = retryable;
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
	}

	public HttpUriRequest toRequest(final RequestConfig requestConfig) {
		final RequestBuilder builder = RequestBuilder.create(this.method);
		builder.setUri(this.uri);
		for (final String name : this.headers.keySet()) {
			final List<String> values = this.headers.get(name);
			for (final String value : values) {
				builder.addHeader(name, value);
			}
		}

		for (final String name : this.params.keySet()) {
			final List<String> values = this.params.get(name);
			for (final String value : values) {
				builder.addParameter(name, value);
			}
		}

		if (this.requestEntity != null) {
			final BasicHttpEntity entity;
			entity = new BasicHttpEntity();
			entity.setContent(this.requestEntity);
			// if the entity contentLength isn't set, transfer-encoding will be set
			// to chunked in org.apache.http.protocol.RequestContent. See gh-1042
			if (contentLength != null) {
				entity.setContentLength(this.contentLength);
			}
			builder.setEntity(entity);
		}

		builder.setConfig(requestConfig);
		return builder.build();
	}

	public RibbonApacheHttpRequest withNewUri(final URI uri) {
		return new RibbonApacheHttpRequest(this.method, uri, this.isRetriable,
				this.headers, this.params, this.requestEntity);
	}

}
