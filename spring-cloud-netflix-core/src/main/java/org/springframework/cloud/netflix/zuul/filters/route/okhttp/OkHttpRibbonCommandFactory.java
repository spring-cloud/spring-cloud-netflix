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

package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import java.util.Collections;
import java.util.Set;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommandFactory;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
public class OkHttpRibbonCommandFactory extends AbstractRibbonCommandFactory {

	private SpringClientFactory clientFactory;
	
	private ZuulProperties zuulProperties;

	public OkHttpRibbonCommandFactory(SpringClientFactory clientFactory, ZuulProperties zuulProperties) {
		this(clientFactory, zuulProperties, Collections.<ZuulFallbackProvider>emptySet());
	}

	public OkHttpRibbonCommandFactory(SpringClientFactory clientFactory, ZuulProperties zuulProperties,
									  Set<ZuulFallbackProvider> zuulFallbackProviders) {
		super(zuulFallbackProviders);
		this.clientFactory = clientFactory;
		this.zuulProperties = zuulProperties;
	}

	@Override
	public OkHttpRibbonCommand create(final RibbonCommandContext context) {
		final String serviceId = context.getServiceId();
		ZuulFallbackProvider fallbackProvider = getFallbackProvider(serviceId);
		final OkHttpLoadBalancingClient client = this.clientFactory.getClient(
				serviceId, OkHttpLoadBalancingClient.class);
		client.setLoadBalancer(this.clientFactory.getLoadBalancer(serviceId));

		return new OkHttpRibbonCommand(serviceId, client, context, zuulProperties, fallbackProvider,
				clientFactory.getClientConfig(serviceId));
	}

}
