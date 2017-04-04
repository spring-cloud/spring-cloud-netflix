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
import org.apache.commons.lang.BooleanUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.feign.ribbon.FeignRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.support.RetryableStatusCodeException;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;

/**
 * An Apache HTTP client which leverages Spring Retry to retry failed requests.
 * @author Ryan Baxter
 */
public class RetryableRibbonLoadBalancingHttpClient extends RibbonLoadBalancingHttpClient implements ServiceInstanceChooser {
	private LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory =
			new LoadBalancedRetryPolicyFactory.NeverRetryFactory();
	public RetryableRibbonLoadBalancingHttpClient(IClientConfig config, ServerIntrospector serverIntrospector, LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	@Override
	public RibbonApacheHttpResponse execute(final RibbonApacheHttpRequest request, final IClientConfig configOverride) throws Exception {
		final RequestConfig.Builder builder = RequestConfig.custom();
		IClientConfig config = configOverride != null ? configOverride : this.config;
		builder.setConnectTimeout(config.get(
				CommonClientConfigKey.ConnectTimeout, this.connectTimeout));
		builder.setSocketTimeout(config.get(
				CommonClientConfigKey.ReadTimeout, this.readTimeout));
		builder.setRedirectsEnabled(config.get(
				CommonClientConfigKey.FollowRedirects, this.followRedirects));

		final RequestConfig requestConfig = builder.build();
		final LoadBalancedRetryPolicy retryPolicy = loadBalancedRetryPolicyFactory.create(this.getClientName(), this);
		RetryCallback retryCallback = new RetryCallback() {
			@Override
			public RibbonApacheHttpResponse doWithRetry(RetryContext context) throws Exception {
				//on retries the policy will choose the server and set it in the context
				//extract the server and update the request being made
				RibbonApacheHttpRequest newRequest = request;
				if(context instanceof LoadBalancedRetryContext) {
					ServiceInstance service = ((LoadBalancedRetryContext)context).getServiceInstance();
					if(service != null) {
						//Reconstruct the request URI using the host and port set in the retry context
						newRequest = newRequest.withNewUri(new URI(service.getUri().getScheme(),
								newRequest.getURI().getUserInfo(), service.getHost(), service.getPort(),
								newRequest.getURI().getPath(), newRequest.getURI().getQuery(),
								newRequest.getURI().getFragment()));
					}
				}
				if (isSecure(configOverride)) {
					final URI secureUri = UriComponentsBuilder.fromUri(newRequest.getUri())
							.scheme("https").build().toUri();
					newRequest = newRequest.withNewUri(secureUri);
				}
				HttpUriRequest httpUriRequest = newRequest.toRequest(requestConfig);
				final HttpResponse httpResponse = RetryableRibbonLoadBalancingHttpClient.this.delegate.execute(httpUriRequest);
				if(retryPolicy.retryableStatusCode(httpResponse.getStatusLine().getStatusCode())) {
					throw new RetryableStatusCodeException(RetryableRibbonLoadBalancingHttpClient.this.clientName,
							httpResponse.getStatusLine().getStatusCode());
				}
				return new RibbonApacheHttpResponse(httpResponse, httpUriRequest.getURI());
			}
		};
		return this.executeWithRetry(request, retryPolicy, retryCallback);
	}

	private RibbonApacheHttpResponse executeWithRetry(RibbonApacheHttpRequest request, LoadBalancedRetryPolicy retryPolicy, RetryCallback<RibbonApacheHttpResponse, IOException> callback) throws Exception {
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
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(RibbonApacheHttpRequest request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, RetryHandler.DEFAULT, null);
	}

	static class RetryPolicy extends FeignRetryPolicy {
		public RetryPolicy(HttpRequest request, LoadBalancedRetryPolicy policy, ServiceInstanceChooser serviceInstanceChooser, String serviceName) {
			super(request, policy, serviceInstanceChooser, serviceName);
		}
	}
}
