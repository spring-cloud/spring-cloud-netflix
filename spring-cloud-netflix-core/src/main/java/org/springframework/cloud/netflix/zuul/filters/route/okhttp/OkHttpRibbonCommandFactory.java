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

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpRibbonRequest;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpRibbonResponse;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommand;

import lombok.RequiredArgsConstructor;

/**
 * @author Spencer Gibb
 */
@RequiredArgsConstructor
public class OkHttpRibbonCommandFactory implements
		RibbonCommandFactory<OkHttpRibbonCommandFactory.OkHttpRibbonCommand> {

	private final SpringClientFactory clientFactory;

	@Override
	public OkHttpRibbonCommand create(final RibbonCommandContext context) {
		final String serviceId = context.getServiceId();
		final OkHttpLoadBalancingClient client = this.clientFactory.getClient(
				serviceId, OkHttpLoadBalancingClient.class);
		client.setLoadBalancer(this.clientFactory.getLoadBalancer(serviceId));

		return new OkHttpRibbonCommand(serviceId, client, context);
	}

	class OkHttpRibbonCommand extends AbstractRibbonCommand<OkHttpLoadBalancingClient, OkHttpRibbonRequest, OkHttpRibbonResponse> {

		public OkHttpRibbonCommand(final String commandKey,
								   final OkHttpLoadBalancingClient client, RibbonCommandContext context) {
			super(commandKey, client, context);
		}

		@Override
		protected OkHttpRibbonRequest createRequest() throws Exception {
			return new OkHttpRibbonRequest(this.context);
		}

	}
}
