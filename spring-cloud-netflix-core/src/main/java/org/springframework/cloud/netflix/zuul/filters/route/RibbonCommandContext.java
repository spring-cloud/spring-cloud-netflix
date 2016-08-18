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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Spencer Gibb
 */
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class RibbonCommandContext {
	@NonNull
	private final String serviceId;
	@NonNull
	private final String method;
	@NonNull
	private final String uri;
	private final Boolean retryable;
	@NonNull
	private final MultiValueMap<String, String> headers;
	@NonNull
	private final MultiValueMap<String, String> params;
	private final InputStream requestEntity;
	@NonNull
	private final List<RibbonRequestCustomizer> requestCustomizers;
	private Long contentLength;

	/**
	 * Kept for backwards compatibility with Spring Cloud Sleuth 1.x versions
	 */
	@Deprecated
	public RibbonCommandContext(String serviceId, String method, String uri,
			Boolean retryable, MultiValueMap<String, String> headers,
			MultiValueMap<String, String> params, InputStream requestEntity) {
		this(serviceId, method, uri, retryable, headers, params, requestEntity,
				new ArrayList<RibbonRequestCustomizer>(), null);
	}

	public URI uri() {
		try {
			return new URI(this.uri);
		}
		catch (URISyntaxException e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}
		return null;
	}

	/**
	 * Use getMethod()
	 * @return
	 */
	@Deprecated
	public String getVerb() {
		return this.method;
	}
}
