/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.ribbon.support;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.util.MultiValueMap;
import com.netflix.client.ClientRequest;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
public abstract class ContextAwareRequest extends ClientRequest implements HttpRequest {
	protected final RibbonCommandContext context;
	private HttpHeaders httpHeaders;

	public ContextAwareRequest(RibbonCommandContext context) {
		this.context = context;
		MultiValueMap<String, String> headers = context.getHeaders();
		this.httpHeaders = new HttpHeaders();
		for(String key : headers.keySet()) {
			this.httpHeaders.put(key, headers.get(key));
		}
		this.uri = context.uri();
		this.isRetriable = context.getRetryable();
		this.loadBalancerKey = context.getLoadBalancerKey();
	}

	public RibbonCommandContext getContext() {
		return context;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(context.getMethod());
	}

	@Override
	public String getMethodValue() {
		return getMethod().name();
	}

	@Override
	public URI getURI() {
		return this.getUri();
	}

	@Override
	public HttpHeaders getHeaders() {
		return httpHeaders;
	}

	protected RibbonCommandContext newContext(URI uri) {
		RibbonCommandContext commandContext = new RibbonCommandContext(this.context.getServiceId(),
				this.context.getMethod(), uri.toString(), this.context.getRetryable(),
				this.context.getHeaders(), this.context.getParams(), this.context.getRequestEntity(),
				this.context.getRequestCustomizers(), this.context.getContentLength(),
				this.context.getLoadBalancerKey());
		return commandContext;
	}
}
