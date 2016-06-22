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

import com.netflix.client.ClientRequest;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;

import java.net.URI;

/**
 * @author Spencer Gibb
 */
public abstract class ContextAwareRequest extends ClientRequest {
	protected final RibbonCommandContext context;

	public ContextAwareRequest(RibbonCommandContext context) {
		this.context = context;
		this.uri = context.uri();
		this.isRetriable = context.getRetryable();
	}

	public RibbonCommandContext getContext() {
		return context;
	}

	protected RibbonCommandContext newContext(URI uri) {
		RibbonCommandContext commandContext = new RibbonCommandContext(this.context.getServiceId(),
				this.context.getMethod(), uri.toString(), this.context.getRetryable(),
				this.context.getHeaders(), this.context.getParams(), this.context.getRequestEntity());
		commandContext.setContentLength(this.context.getContentLength());
		return commandContext;
	}
}
