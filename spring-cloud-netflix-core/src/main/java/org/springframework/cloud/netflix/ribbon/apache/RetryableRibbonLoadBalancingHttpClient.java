/*
 * Copyright 2013-2018 the original author or authors.
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
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.RibbonRecoveryCallback;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.client.loadbalancer.InterceptorRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.client.ClientException;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;

/**
 * An Apache HTTP client which leverages Spring Retry to retry failed requests.
 * @author Ryan Baxter
 * @author Gang Li
 */
public class RetryableRibbonLoadBalancingHttpClient extends RibbonLoadBalancingHttpClient {
	private LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory = new LoadBalancedRetryPolicyFactory.NeverRetryFactory();
	private LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory =
		new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory();
	private LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory =
		new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory();

	@Deprecated
	//TODO remove in 2.0.x
	public RetryableRibbonLoadBalancingHttpClient(IClientConfig config,
			ServerIntrospector serverIntrospector,
			LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	@Deprecated
	//TODO remove in 2.0.x
	public RetryableRibbonLoadBalancingHttpClient(CloseableHttpClient delegate,
			IClientConfig config, ServerIntrospector serverIntrospector,
			LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(delegate, config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	@Deprecated
	//TODO remove in 2.0.x
	public RetryableRibbonLoadBalancingHttpClient(CloseableHttpClient delegate,
												  IClientConfig config, ServerIntrospector serverIntrospector,
												  LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
												  LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory) {
		super(delegate, config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory;
	}

	public RetryableRibbonLoadBalancingHttpClient(CloseableHttpClient delegate,
												  IClientConfig config, ServerIntrospector serverIntrospector,
												  LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
												  LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
												  LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) {
		super(delegate, config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory;
		this.loadBalancedRetryListenerFactory = loadBalancedRetryListenerFactory;
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
		RetryCallback<RibbonApacheHttpResponse, Exception> retryCallback = new RetryCallback<RibbonApacheHttpResponse, Exception>() {
			@Override
			public RibbonApacheHttpResponse doWithRetry(RetryContext context) throws Exception {
				//on retries the policy will choose the server and set it in the context
				//extract the server and update the request being made
				RibbonApacheHttpRequest newRequest = request;
				if(context instanceof LoadBalancedRetryContext) {
					ServiceInstance service = ((LoadBalancedRetryContext)context).getServiceInstance();
					validateServiceInstance(service);
					//Reconstruct the request URI using the host and port set in the retry context
					newRequest = newRequest.withNewUri(UriComponentsBuilder.newInstance().host(service.getHost())
							.scheme(service.getUri().getScheme()).userInfo(newRequest.getURI().getUserInfo())
							.port(service.getPort()).path(newRequest.getURI().getPath())
							.query(newRequest.getURI().getQuery()).fragment(newRequest.getURI().getFragment())
							.build().encode().toUri());
				}
				newRequest = getSecureRequest(newRequest, configOverride);
				HttpUriRequest httpUriRequest = newRequest.toRequest(requestConfig);
				final HttpResponse httpResponse = RetryableRibbonLoadBalancingHttpClient.this.delegate.execute(httpUriRequest);
				if(retryPolicy.retryableStatusCode(httpResponse.getStatusLine().getStatusCode())) {
					throw new HttpClientStatusCodeException(RetryableRibbonLoadBalancingHttpClient.this.clientName,
							httpResponse, HttpClientUtils.createEntity(httpResponse), httpUriRequest.getURI());
				}
				return new RibbonApacheHttpResponse(httpResponse, httpUriRequest.getURI());
			}
		};
		RibbonRecoveryCallback<RibbonApacheHttpResponse, HttpResponse> recoveryCallback = new RibbonRecoveryCallback<RibbonApacheHttpResponse, HttpResponse>() {
			@Override
			protected RibbonApacheHttpResponse createResponse(HttpResponse response, URI uri) {
				return new RibbonApacheHttpResponse(response, uri);
			}
 		};
		return this.executeWithRetry(request, retryPolicy, retryCallback, recoveryCallback);
	}
	
	@Override
	public boolean isClientRetryable(ContextAwareRequest request) {
		return request!= null && isRequestRetryable(request);
	}
	
	private boolean isRequestRetryable(ContextAwareRequest request) {
		return request.getContext() == null ? true :
			BooleanUtils.toBooleanDefaultIfNull(request.getContext().getRetryable(), true);
	}

	private RibbonApacheHttpResponse executeWithRetry(RibbonApacheHttpRequest request, LoadBalancedRetryPolicy retryPolicy,
													  RetryCallback<RibbonApacheHttpResponse, Exception> callback,
													  RecoveryCallback<RibbonApacheHttpResponse> recoveryCallback) throws Exception {
		RetryTemplate retryTemplate = new RetryTemplate();
		boolean retryable = isRequestRetryable(request);
		retryTemplate.setRetryPolicy(retryPolicy == null || !retryable ? new NeverRetryPolicy()
				: new RetryPolicy(request, retryPolicy, this, this.getClientName()));
		BackOffPolicy backOffPolicy = loadBalancedBackOffPolicyFactory.createBackOffPolicy(this.getClientName());
		retryTemplate.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		RetryListener[] retryListeners = this.loadBalancedRetryListenerFactory.createRetryListeners(this.getClientName());
		if (retryListeners != null && retryListeners.length != 0) {
			retryTemplate.setListeners(retryListeners);
		}
		return retryTemplate.execute(callback, recoveryCallback);
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(RibbonApacheHttpRequest request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, RetryHandler.DEFAULT, null);
	}

	static class RetryPolicy extends InterceptorRetryPolicy {
		public RetryPolicy(HttpRequest request, LoadBalancedRetryPolicy policy,
				ServiceInstanceChooser serviceInstanceChooser, String serviceName) {
			super(request, policy, serviceInstanceChooser, serviceName);
		}
	}
}
