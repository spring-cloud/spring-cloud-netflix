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
package org.springframework.cloud.netflix.ribbon.okhttp;

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
 * An OK HTTP client which leverages Spring Retry to retry failed request.
 * @author Ryan Baxter
 */
public class RetryableOkHttpLoadBalancingClient extends OkHttpLoadBalancingClient implements ServiceInstanceChooser {

	private LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;

	public RetryableOkHttpLoadBalancingClient(IClientConfig config, ServerIntrospector serverIntrospector,
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
	public OkHttpRibbonResponse execute(OkHttpRibbonRequest request, IClientConfig requestConfig) throws Exception {
		try {
			return Observable.create(new RetryableOkHttpClientExecutionObservable(request, requestConfig))
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
	public Observable<OkHttpRibbonResponse> getExecutionWithLoadBalancerObservable(final OkHttpRibbonRequest request,
																				   final IClientConfig requestConfig) {
		return Observable.defer(new Func0<Observable<OkHttpRibbonResponse>>() {

			@Override
			public Observable<OkHttpRibbonResponse> call() {
				String serviceId = getClientName();
				ServiceInstance service = choose(serviceId);
				try {
					OkHttpRibbonRequest newRequest = reconstruct(request, service);
					return Observable.create(new RetryableOkHttpClientExecutionObservable(newRequest, requestConfig));
				} catch (URISyntaxException e) {
					return Observable.error(e);
				}
			}
		});
	}

	private OkHttpRibbonRequest reconstruct(OkHttpRibbonRequest request, ServiceInstance service)
			throws URISyntaxException {
		return request.withNewUri(new URI(service.getUri().getScheme(),
				request.getURI().getUserInfo(), service.getHost(), service.getPort(),
				request.getURI().getPath(), request.getURI().getQuery(),
				request.getURI().getFragment()));
	}

	private class RetryableOkHttpClientExecutionObservable extends RetryableClientObservable<OkHttpRibbonRequest, OkHttpRibbonResponse> {

		public RetryableOkHttpClientExecutionObservable(OkHttpRibbonRequest request, IClientConfig requestConfig) {
			super(clientName, RetryableOkHttpLoadBalancingClient.this, loadBalancedRetryPolicyFactory, request, requestConfig);
		}

		@Override
		protected OkHttpRibbonRequest reconstruct(OkHttpRibbonRequest request, ServiceInstance service)
				throws URISyntaxException {
			return RetryableOkHttpLoadBalancingClient.this.reconstruct(request, service);
		}

		@Override
		protected OkHttpRibbonResponse executeInternal(OkHttpRibbonRequest request, IClientConfig requestConfig)
				throws Exception {
			return RetryableOkHttpLoadBalancingClient.this.executeInternal(request, requestConfig);
		}
	}
}
