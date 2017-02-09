/*
 *
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

package org.springframework.cloud.netflix.ribbon.support;

import java.io.IOException;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.InterceptorRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.feign.ribbon.FeignRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

/**
 * A load balancing client which uses Spring Retry to retry failed requests.
 * @author Ryan Baxter
 */
public abstract class RetryableLoadBalancingClient<S extends ContextAwareRequest, T extends IResponse, D>
		extends AbstractLoadBalancingClient<S, T, D> implements ServiceInstanceChooser {

	protected LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory =
			new LoadBalancedRetryPolicyFactory.NeverRetryFactory();

	@Deprecated
	public RetryableLoadBalancingClient() {
		super();
	}

	@Deprecated
	public RetryableLoadBalancingClient(final ILoadBalancer lb) {
		super(lb);
	}

	public RetryableLoadBalancingClient(IClientConfig config, ServerIntrospector serverIntrospector) {
		super(config, serverIntrospector);
	}

	public RetryableLoadBalancingClient(D delegate, IClientConfig config, ServerIntrospector serverIntrospector) {
		super(delegate, config, serverIntrospector);
	}

	public RetryableLoadBalancingClient(IClientConfig iClientConfig, ServerIntrospector serverIntrospector,
										 LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		this(iClientConfig, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	/**
	 * Executes a {@link S} using Spring Retry.
	 * @param request The request to execute.
	 * @param callback The retry callback to use.
	 * @return The response.
	 * @throws Exception Thrown if there is an error making the request and a retry cannot be completed successfully.
	 */
	protected T executeWithRetry(S request, RetryCallback<T, IOException> callback) throws Exception {
		LoadBalancedRetryPolicy retryPolicy = loadBalancedRetryPolicyFactory.create(this.getClientName(), this);
		RetryTemplate retryTemplate = new RetryTemplate();
		boolean retryable = request.getContext() == null ? true :
				BooleanUtils.toBooleanDefaultIfNull(request.getContext().getRetryable(), true);
		retryTemplate.setRetryPolicy(retryPolicy == null || !retryable ? new NeverRetryPolicy()
				: new RetryPolicy(request, retryPolicy, this, this.getClientName()));
		return retryTemplate.execute(callback);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		Server server = this.getLoadBalancer().chooseServer(serviceId);
		return new RibbonLoadBalancerClient.RibbonServer(serviceId,
				server);
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(S request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, RetryHandler.DEFAULT, null);
	}

	static class RetryPolicy extends FeignRetryPolicy {
		public RetryPolicy(HttpRequest request, LoadBalancedRetryPolicy policy, ServiceInstanceChooser serviceInstanceChooser, String serviceName) {
			super(request, policy, serviceInstanceChooser, serviceName);
		}
	}

}
