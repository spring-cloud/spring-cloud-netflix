/*
 * Copyright 2013-2017 the original author or authors.
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
package org.springframework.cloud.netflix.ribbon.apache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.support.RetryableClientObservable;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import rx.Observable;
import rx.functions.Func0;

/**
 * An Apache HTTP client which leverages Spring Retry to retry failed requests.
 * @author Ryan Baxter
 */
public class RetryableRibbonLoadBalancingHttpClient extends RibbonLoadBalancingHttpClient implements ServiceInstanceChooser {

	private LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory =
			new LoadBalancedRetryPolicyFactory.NeverRetryFactory();

	public RetryableRibbonLoadBalancingHttpClient(IClientConfig config, ServerIntrospector serverIntrospector,
												  LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		Server server = this.getLoadBalancer().chooseServer(serviceId);
		return new RibbonLoadBalancerClient.RibbonServer(serviceId, server);
	}

	@Override
	public RibbonApacheHttpResponse execute(RibbonApacheHttpRequest request, IClientConfig requestConfig) throws Exception {
		try {
			return Observable.create(new RetryableHttpClientExecutionObservable(request, requestConfig))
					.toBlocking()
					.single();
		} catch (Exception e) {
			Throwable t = e.getCause();
			if (t instanceof IOException) {
				throw (IOException) t;
			} else {
				throw new IOException(e);
			}
		}
	}

	@Override
	public Observable<RibbonApacheHttpResponse> getExecutionWithLoadBalancerObservable(final RibbonApacheHttpRequest request,
																					   final IClientConfig requestConfig) {
		return Observable.defer(new Func0<Observable<RibbonApacheHttpResponse>>() {

			@Override
			public Observable<RibbonApacheHttpResponse> call() {
				String serviceId = getClientName();
				ServiceInstance service = choose(serviceId);
				try {
					RibbonApacheHttpRequest newRequest = reconstruct(request, service);
					return Observable.create(new RetryableHttpClientExecutionObservable(newRequest, requestConfig));
				} catch (URISyntaxException e) {
					return Observable.error(e);
				}
			}
		});
	}

	private RibbonApacheHttpRequest reconstruct(RibbonApacheHttpRequest request, ServiceInstance service)
			throws URISyntaxException {
		return request.withNewUri(new URI(service.getUri().getScheme(),
				request.getURI().getUserInfo(), service.getHost(), service.getPort(),
				request.getURI().getPath(), request.getURI().getQuery(),
				request.getURI().getFragment()));
	}

	private class RetryableHttpClientExecutionObservable extends RetryableClientObservable<RibbonApacheHttpRequest, RibbonApacheHttpResponse> {

		public RetryableHttpClientExecutionObservable(RibbonApacheHttpRequest request, IClientConfig requestConfig) {
			super(clientName, RetryableRibbonLoadBalancingHttpClient.this, loadBalancedRetryPolicyFactory, request, requestConfig);
		}

		@Override
		protected RibbonApacheHttpRequest reconstruct(RibbonApacheHttpRequest request, ServiceInstance service)
				throws URISyntaxException {
			return RetryableRibbonLoadBalancingHttpClient.this.reconstruct(request, service);
		}

		@Override
		protected RibbonApacheHttpResponse executeInternal(RibbonApacheHttpRequest request, IClientConfig requestConfig)
				throws Exception {
			return RetryableRibbonLoadBalancingHttpClient.this.executeInternal(request, requestConfig);
		}
	}
}
