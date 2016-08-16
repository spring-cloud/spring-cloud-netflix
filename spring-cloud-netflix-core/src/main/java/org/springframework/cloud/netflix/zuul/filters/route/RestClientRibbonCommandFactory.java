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

package org.springframework.cloud.netflix.zuul.filters.route;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;

import com.netflix.client.http.HttpRequest;
import com.netflix.niws.client.http.RestClient;

import lombok.RequiredArgsConstructor;

/**
 * @author Spencer Gibb
 */
@RequiredArgsConstructor
public class RestClientRibbonCommandFactory
		implements RibbonCommandFactory<RestClientRibbonCommand> {

	private final SpringClientFactory clientFactory;

	private final ZuulProperties zuulProperties;

	@Override
	@SuppressWarnings("deprecation")
	public RestClientRibbonCommand create(RibbonCommandContext context) {
		RestClient restClient = this.clientFactory.getClient(context.getServiceId(),
				RestClient.class);
		return new RestClientRibbonCommand(context.getServiceId(), restClient, context,
				this.zuulProperties);
	}

	public SpringClientFactory getClientFactory() {
		return clientFactory;
	}

	protected static HttpRequest.Verb getVerb(String method) {
		return RestClientRibbonCommand.getVerb(method);
	}
}
