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

package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpRibbonRequest;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpRibbonResponse;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommand;
import com.netflix.client.config.IClientConfig;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
public class OkHttpRibbonCommand extends AbstractRibbonCommand<OkHttpLoadBalancingClient, OkHttpRibbonRequest, OkHttpRibbonResponse> {

	public OkHttpRibbonCommand(final String commandKey,
								final OkHttpLoadBalancingClient client,
								final RibbonCommandContext context,
								final ZuulProperties zuulProperties) {
		super(commandKey, client, context, zuulProperties);
	}

	public OkHttpRibbonCommand(final String commandKey,
							   final OkHttpLoadBalancingClient client,
							   final RibbonCommandContext context,
							   final ZuulProperties zuulProperties,
							   final ZuulFallbackProvider zuulFallbackProvider) {
		super(commandKey, client, context, zuulProperties, zuulFallbackProvider);
	}

	public OkHttpRibbonCommand(final String commandKey,
							   final OkHttpLoadBalancingClient client,
							   final RibbonCommandContext context,
							   final ZuulProperties zuulProperties,
							   final ZuulFallbackProvider zuulFallbackProvider,
							   final IClientConfig config) {
		super(commandKey, client, context, zuulProperties, zuulFallbackProvider, config);
	}

	@Override
	protected OkHttpRibbonRequest createRequest() throws Exception {
		return new OkHttpRibbonRequest(this.context);
	}

}
