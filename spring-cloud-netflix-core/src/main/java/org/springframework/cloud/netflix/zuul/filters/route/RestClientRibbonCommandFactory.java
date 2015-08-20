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

import com.netflix.client.http.HttpRequest;
import com.netflix.niws.client.http.RestClient;
import lombok.SneakyThrows;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

/**
 * @author Spencer Gibb
 */
public class RestClientRibbonCommandFactory implements RibbonCommandFactory<RestClientRibbonCommand> {

	private final SpringClientFactory clientFactory;

	public RestClientRibbonCommandFactory(SpringClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	@SuppressWarnings("deprecation")
	@SneakyThrows
	public RestClientRibbonCommand create(RibbonCommandContext context) {
		RestClient restClient = this.clientFactory.getClient(context.getServiceId(),
				RestClient.class);
		return new RestClientRibbonCommand(
				context.getServiceId(), restClient, getVerb(context.getVerb()),
				context.getUri(), context.getRetryable(), context.getHeaders(),
				context.getParams(), context.getRequestEntity());
	}

	protected SpringClientFactory getClientFactory() {
		return clientFactory;
	}

	protected static HttpRequest.Verb getVerb(String sMethod) {
		if (sMethod == null)
			return HttpRequest.Verb.GET;
		try {
			return HttpRequest.Verb.valueOf(sMethod.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return HttpRequest.Verb.GET;
		}
	}
}
