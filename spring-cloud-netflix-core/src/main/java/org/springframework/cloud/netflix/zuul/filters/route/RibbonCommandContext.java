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

import java.io.InputStream;

import org.springframework.util.MultiValueMap;

/**
 * @author Spencer Gibb
 */
public class RibbonCommandContext {
	private final String serviceId;
	private final String verb;
	private final String uri;
	private final Boolean retryable;
	private final MultiValueMap<String, String> headers;
	private final MultiValueMap<String, String> params;
	private final InputStream requestEntity;

	public RibbonCommandContext(String serviceId, String verb, String uri,
			Boolean retryable, MultiValueMap<String, String> headers,
			MultiValueMap<String, String> params, InputStream requestEntity) {
		this.serviceId = serviceId;
		this.verb = verb;
		this.uri = uri;
		this.retryable = retryable;
		this.headers = headers;
		this.params = params;
		this.requestEntity = requestEntity;
	}

	public String getServiceId() {
		return this.serviceId;
	}

	public String getVerb() {
		return this.verb;
	}

	public String getUri() {
		return this.uri;
	}

	public Boolean getRetryable() {
		return this.retryable;
	}

	public MultiValueMap<String, String> getHeaders() {
		return this.headers;
	}

	public MultiValueMap<String, String> getParams() {
		return this.params;
	}

	public InputStream getRequestEntity() {
		return this.requestEntity;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RibbonCommandContext))
			return false;
		final RibbonCommandContext other = (RibbonCommandContext) o;
		final Object this$serviceId = this.serviceId;
		final Object other$serviceId = other.serviceId;
		if (this$serviceId == null ?
				other$serviceId != null :
				!this$serviceId.equals(other$serviceId))
			return false;
		final Object this$verb = this.verb;
		final Object other$verb = other.verb;
		if (this$verb == null ? other$verb != null : !this$verb.equals(other$verb))
			return false;
		final Object this$uri = this.uri;
		final Object other$uri = other.uri;
		if (this$uri == null ? other$uri != null : !this$uri.equals(other$uri))
			return false;
		final Object this$retryable = this.retryable;
		final Object other$retryable = other.retryable;
		if (this$retryable == null ?
				other$retryable != null :
				!this$retryable.equals(other$retryable))
			return false;
		final Object this$headers = this.headers;
		final Object other$headers = other.headers;
		if (this$headers == null ?
				other$headers != null :
				!this$headers.equals(other$headers))
			return false;
		final Object this$params = this.params;
		final Object other$params = other.params;
		if (this$params == null ?
				other$params != null :
				!this$params.equals(other$params))
			return false;
		final Object this$requestEntity = this.requestEntity;
		final Object other$requestEntity = other.requestEntity;
		if (this$requestEntity == null ?
				other$requestEntity != null :
				!this$requestEntity.equals(other$requestEntity))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $serviceId = this.serviceId;
		result = result * PRIME + ($serviceId == null ? 0 : $serviceId.hashCode());
		final Object $verb = this.verb;
		result = result * PRIME + ($verb == null ? 0 : $verb.hashCode());
		final Object $uri = this.uri;
		result = result * PRIME + ($uri == null ? 0 : $uri.hashCode());
		final Object $retryable = this.retryable;
		result = result * PRIME + ($retryable == null ? 0 : $retryable.hashCode());
		final Object $headers = this.headers;
		result = result * PRIME + ($headers == null ? 0 : $headers.hashCode());
		final Object $params = this.params;
		result = result * PRIME + ($params == null ? 0 : $params.hashCode());
		final Object $requestEntity = this.requestEntity;
		result =
				result * PRIME + ($requestEntity == null ? 0 : $requestEntity.hashCode());
		return result;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext(serviceId="
				+ this.serviceId + ", verb=" + this.verb + ", uri=" + this.uri
				+ ", retryable=" + this.retryable + ", headers=" + this.headers
				+ ", params=" + this.params + ", requestEntity=" + this.requestEntity
				+ ")";
	}
}
