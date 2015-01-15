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
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

/**
 * @author Spencer Gibb
 */
public class RibbonInterceptor implements ClientHttpRequestInterceptor {

	private LoadBalancerClient loadBalancer;

	public RibbonInterceptor(LoadBalancerClient loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
			final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		String serviceName = originalUri.getHost();
		return this.loadBalancer.execute(serviceName,
				new LoadBalancerRequest<ClientHttpResponse>() {

					@Override
					public ClientHttpResponse apply(final ServiceInstance instance)
							throws Exception {
						HttpRequest serviceRequest = new ServiceRequestWrapper(request,
								instance);
						return execution.execute(serviceRequest, body);
					}

				});
	}

	private class ServiceRequestWrapper extends HttpRequestWrapper {

		private final ServiceInstance instance;

		public ServiceRequestWrapper(HttpRequest request, ServiceInstance instance) {
			super(request);
			this.instance = instance;
		}

		@Override
		public URI getURI() {
			URI uri = RibbonInterceptor.this.loadBalancer.reconstructURI(this.instance,
					getRequest().getURI());
			return uri;
		}

	}

}
