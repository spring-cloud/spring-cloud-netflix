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

import java.util.Collections;
import java.util.Set;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommandFactory;

import com.netflix.client.http.HttpRequest;
import com.netflix.niws.client.http.RestClient;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
public class RestClientRibbonCommandFactory extends AbstractRibbonCommandFactory {

	private SpringClientFactory clientFactory;

	private ZuulProperties zuulProperties;

	public RestClientRibbonCommandFactory(SpringClientFactory clientFactory) {
		this(clientFactory, new ZuulProperties(), Collections.<ZuulFallbackProvider>emptySet());
	}

	public RestClientRibbonCommandFactory(SpringClientFactory clientFactory,
										  ZuulProperties zuulProperties,
										  Set<ZuulFallbackProvider> zuulFallbackProviders) {
		super(zuulFallbackProviders);
		this.clientFactory = clientFactory;
		this.zuulProperties = zuulProperties;
	}

	@Override
	@SuppressWarnings("deprecation")
	public RestClientRibbonCommand create(RibbonCommandContext context) {
		String serviceId = context.getServiceId();
		ZuulFallbackProvider fallbackProvider = getFallbackProvider(serviceId);
		RestClient restClient = this.clientFactory.getClient(serviceId,
				RestClient.class);
		return new RestClientRibbonCommand(context.getServiceId(), restClient, context,
				this.zuulProperties, fallbackProvider, clientFactory.getClientConfig(serviceId));
	}

	public SpringClientFactory getClientFactory() {
		return clientFactory;
	}

	public void setZuulProperties(ZuulProperties zuulProperties) {
		this.zuulProperties = zuulProperties;
	}

	protected static HttpRequest.Verb getVerb(String method) {
		return RestClientRibbonCommand.getVerb(method);
	}

}
