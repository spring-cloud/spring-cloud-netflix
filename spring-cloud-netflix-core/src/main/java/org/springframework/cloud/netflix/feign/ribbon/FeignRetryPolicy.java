/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.feign.ribbon;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.InterceptorRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

/**
 * @author Ryan Baxter
 */
public class FeignRetryPolicy extends InterceptorRetryPolicy {
	private HttpRequest request;
	private String serviceId;
	public FeignRetryPolicy(HttpRequest request, LoadBalancedRetryPolicy policy, ServiceInstanceChooser serviceInstanceChooser, String serviceName) {
		super(request, policy, serviceInstanceChooser, serviceName);
		this.request = request;
		this.serviceId = serviceName;
	}

	@Override
	public boolean canRetry(RetryContext context) {
		/*
		 * In InterceptorRetryPolicy.canRetry we ask the LoadBalancer to choose a server if one is not
		 * set in the retry context and then return true.  RetryTemplat calls the canRetry method of
		 * the policy even on its first execution.  So the fact that we didnt have a service instance set
		 * in the RetryContext signaled that it was the first execution and we should return true.
		 *
		 * In the Feign scenario, Feign as actually already queried the load balancer for a service instance
		 * and we set that service instance in the context when we call the open method of the policy.  So in
		 * the Feign case we just return true if the retry count is 0 indicating we haven't yet made a failed
		 * request.
		 */
		if(context.getRetryCount() == 0) {
			return true;
		}
		return super.canRetry(context);
	}

	@Override
	public RetryContext open(RetryContext parent) {
		/*
		 * With Feign (unlike Ribbon) the request already has the URI for the service instance
		 * we are going to make the request to, so extract that information and set the service
		 * instance in the context.  In the Ribbon scenario the URI in the request object still has
		 * the service id so we choose and set the service instance later on.
		 */
		LoadBalancedRetryContext context = new LoadBalancedRetryContext(parent, this.request);
		context.setServiceInstance(new FeignRetryPolicyServiceInstance(serviceId, request));
		return context;
	}

	class FeignRetryPolicyServiceInstance implements ServiceInstance {

		private String serviceId;
		private HttpRequest request;
		private Map<String, String> metadata;

		FeignRetryPolicyServiceInstance(String serviceId, HttpRequest request) {
			this.serviceId = serviceId;
			this.request = request;
			this.metadata = new HashMap<String, String>();
		}

		@Override
		public String getServiceId() {
			return serviceId;
		}

		@Override
		public String getHost() {
			return request.getURI().getHost();
		}

		@Override
		public int getPort() {
			return request.getURI().getPort();
		}

		@Override
		public boolean isSecure() {
			return "https".equals(request.getURI().getScheme());
		}

		@Override
		public URI getUri() {
			return request.getURI();
		}

		@Override
		public Map<String, String> getMetadata() {
			return metadata;
		}
	}
}