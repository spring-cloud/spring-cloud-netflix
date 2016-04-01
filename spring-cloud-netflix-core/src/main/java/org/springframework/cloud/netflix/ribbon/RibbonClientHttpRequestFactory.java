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

package org.springframework.cloud.netflix.ribbon;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpRequest;
import com.netflix.niws.client.http.RestClient;

/**
 * @author Spencer Gibb
 */
public class RibbonClientHttpRequestFactory implements ClientHttpRequestFactory {

	private final SpringClientFactory clientFactory;

	public RibbonClientHttpRequestFactory(SpringClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	@SuppressWarnings("deprecation")
	public ClientHttpRequest createRequest(URI originalUri, HttpMethod httpMethod)
			throws IOException {
		String serviceId = originalUri.getHost();
		if (serviceId == null) {
			throw new IOException(
					"Invalid hostname in the URI [" + originalUri.toASCIIString() + "]");
		}
		IClientConfig clientConfig = this.clientFactory.getClientConfig(serviceId);
		RestClient client = this.clientFactory.getClient(serviceId, RestClient.class);
		HttpRequest.Verb verb = HttpRequest.Verb.valueOf(httpMethod.name());

		return new RibbonHttpRequest(originalUri, verb, client, clientConfig);
	}

}
