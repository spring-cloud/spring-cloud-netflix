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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
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

    private LoadBalancerClient loadBalancer;

	public RibbonClientHttpRequestFactory(SpringClientFactory clientFactory, LoadBalancerClient loadBalancer) {
		this.clientFactory = clientFactory;
        this.loadBalancer = loadBalancer;
	}

	@Override
	@SuppressWarnings("deprecation")
	public ClientHttpRequest createRequest(URI originalUri, HttpMethod httpMethod)
			throws IOException {
        String serviceId = originalUri.getHost();
        ServiceInstance instance = loadBalancer.choose(serviceId);
		if (instance == null) {
			throw new IllegalStateException("No instances available for "+serviceId);
		}
        URI uri = loadBalancer.reconstructURI(instance, originalUri);
        //@formatter:off
		IClientConfig clientConfig = clientFactory.getClientConfig(instance.getServiceId());
		RestClient client = clientFactory.getClient(instance.getServiceId(), RestClient.class);
		HttpRequest.Verb verb = HttpRequest.Verb.valueOf(httpMethod.name());
        //@formatter:on
		return new RibbonHttpRequest(uri, verb, client, clientConfig);
	}

}
