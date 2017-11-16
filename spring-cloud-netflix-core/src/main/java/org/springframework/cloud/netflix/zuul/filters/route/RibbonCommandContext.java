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

package org.springframework.cloud.netflix.zuul.filters.route;

import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.support.ResettableServletInputStreamWrapper;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Spencer Gibb
 * @author Yongsung Yoon
 */
public class RibbonCommandContext {
	private final String serviceId;
	private final String method;
	private final String uri;
	private final Boolean retryable;
	private final MultiValueMap<String, String> headers;
	private final MultiValueMap<String, String> params;
	private final List<RibbonRequestCustomizer> requestCustomizers;
	private InputStream requestEntity;
	private Long contentLength;
	private Object loadBalancerKey;

	/**
	 * Kept for backwards compatibility with Spring Cloud Sleuth 1.x versions
	 */
	@Deprecated
	public RibbonCommandContext(String serviceId, String method,
								String uri, Boolean retryable, MultiValueMap<String, String> headers,
								MultiValueMap<String, String> params, InputStream requestEntity) {
		this(serviceId, method, uri, retryable, headers, params, requestEntity,
			new ArrayList<RibbonRequestCustomizer>(), null, null);
	}

	public RibbonCommandContext(String serviceId, String method, String uri,
								Boolean retryable, MultiValueMap<String, String> headers,
								MultiValueMap<String, String> params, InputStream requestEntity,
								List<RibbonRequestCustomizer> requestCustomizers) {
		this(serviceId, method, uri, retryable, headers, params, requestEntity,
			requestCustomizers, null, null);
	}

	public RibbonCommandContext(String serviceId, String method, String uri,
								Boolean retryable, MultiValueMap<String, String> headers,
								MultiValueMap<String, String> params, InputStream requestEntity,
								List<RibbonRequestCustomizer> requestCustomizers, Long contentLength) {
		this(serviceId, method, uri, retryable, headers, params, requestEntity,
			requestCustomizers, contentLength, null);
	}

	public RibbonCommandContext(String serviceId, String method, String uri,
								Boolean retryable, MultiValueMap<String, String> headers,
								MultiValueMap<String, String> params, InputStream requestEntity,
								List<RibbonRequestCustomizer> requestCustomizers, Long contentLength,
								Object loadBalancerKey) {
		Assert.notNull(serviceId, "serviceId may not be null");
		Assert.notNull(method, "method may not be null");
		Assert.notNull(uri, "uri may not be null");
		Assert.notNull(headers, "headers may not be null");
		Assert.notNull(params, "params may not be null");
		Assert.notNull(requestCustomizers, "requestCustomizers may not be null");
		this.serviceId = serviceId;
		this.method = method;
		this.uri = uri;
		this.retryable = retryable;
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
		this.requestCustomizers = requestCustomizers;
		this.contentLength = contentLength;
		this.loadBalancerKey = loadBalancerKey;
	}

	public URI uri() {
		try {
			return new URI(this.uri);
		} catch (URISyntaxException e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}
		return null;
	}

	/**
	 * Use getMethod()
	 *
	 * @return
	 */
	@Deprecated
	public String getVerb() {
		return this.method;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	public Boolean getRetryable() {
		return retryable;
	}

	public MultiValueMap<String, String> getHeaders() {
		return headers;
	}

	public MultiValueMap<String, String> getParams() {
		return params;
	}

	public InputStream getRequestEntity() {
		if (requestEntity == null) {
			return requestEntity;
		}

		try {
			if (!(requestEntity instanceof ResettableServletInputStreamWrapper)) {
				requestEntity = new ResettableServletInputStreamWrapper(
					StreamUtils.copyToByteArray(requestEntity));
			}
			requestEntity.reset();
		} finally {
			return requestEntity;
		}
	}

	public List<RibbonRequestCustomizer> getRequestCustomizers() {
		return requestCustomizers;
	}

	public Long getContentLength() {
		return contentLength;
	}

	public void setContentLength(Long contentLength) {
		this.contentLength = contentLength;
	}

	public Object getLoadBalancerKey() {
		return loadBalancerKey;
	}

	public void setLoadBalancerKey(Object loadBalancerKey) {
		this.loadBalancerKey = loadBalancerKey;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RibbonCommandContext that = (RibbonCommandContext) o;
		return Objects.equals(serviceId, that.serviceId) && Objects
			.equals(method, that.method) && Objects.equals(uri, that.uri)
			&& Objects.equals(retryable, that.retryable) && Objects
			.equals(headers, that.headers) && Objects
			.equals(params, that.params) && Objects
			.equals(requestEntity, that.requestEntity) && Objects
			.equals(requestCustomizers, that.requestCustomizers) && Objects
			.equals(contentLength, that.contentLength) && Objects
			.equals(loadBalancerKey, that.loadBalancerKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceId, method, uri, retryable, headers, params,
			requestEntity, requestCustomizers, contentLength, loadBalancerKey);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("RibbonCommandContext{");
		sb.append("serviceId='").append(serviceId).append('\'');
		sb.append(", method='").append(method).append('\'');
		sb.append(", uri='").append(uri).append('\'');
		sb.append(", retryable=").append(retryable);
		sb.append(", headers=").append(headers);
		sb.append(", params=").append(params);
		sb.append(", requestEntity=").append(requestEntity);
		sb.append(", requestCustomizers=").append(requestCustomizers);
		sb.append(", contentLength=").append(contentLength);
		sb.append(", loadBalancerKey=").append(loadBalancerKey);
		sb.append('}');
		return sb.toString();
	}
}
