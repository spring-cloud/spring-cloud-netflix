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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.URI;
import org.apache.commons.lang.BooleanUtils;
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
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;

/**
 * An OK HTTP client which leverages Spring Retry to retry failed request.
 * @author Ryan Baxter
 * @author Gang Li
 */
public class RetryableOkHttpLoadBalancingClient extends OkHttpLoadBalancingClient {

	private LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;
	private LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory =
		new LoadBalancedBackOffPolicyFactory.NoBackOffPolicyFactory();
	private LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory =
		new LoadBalancedRetryListenerFactory.DefaultRetryListenerFactory();

	@Deprecated
	//TODO remove in 2.0.x
	public RetryableOkHttpLoadBalancingClient(OkHttpClient delegate, IClientConfig config, ServerIntrospector serverIntrospector,
									 LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(delegate, config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
	}

	@Deprecated
	//TODO remove in 2.0.x
	public RetryableOkHttpLoadBalancingClient(OkHttpClient delegate, IClientConfig config, ServerIntrospector serverIntrospector,
											  LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
											  LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory) {
		super(delegate, config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory;
	}

	public RetryableOkHttpLoadBalancingClient(OkHttpClient delegate, IClientConfig config, ServerIntrospector serverIntrospector,
											  LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory,
											  LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory,
											  LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory) {
		super(delegate, config, serverIntrospector);
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.loadBalancedBackOffPolicyFactory = loadBalancedBackOffPolicyFactory;
		this.loadBalancedRetryListenerFactory = loadBalancedRetryListenerFactory;
	}

	@Override
	public boolean isClientRetryable(ContextAwareRequest request) {
		return request!= null && isRequestRetryable(request);
	}
	
	private boolean isRequestRetryable(ContextAwareRequest request) {
		return request.getContext() == null ? true :
			BooleanUtils.toBooleanDefaultIfNull(request.getContext().getRetryable(), true);
	}
	
	private OkHttpRibbonResponse executeWithRetry(OkHttpRibbonRequest request, LoadBalancedRetryPolicy retryPolicy,
												  RetryCallback<OkHttpRibbonResponse, Exception> callback,
												  RecoveryCallback<OkHttpRibbonResponse> recoveryCallback) throws Exception {
		RetryTemplate retryTemplate = new RetryTemplate();
		BackOffPolicy backOffPolicy = loadBalancedBackOffPolicyFactory.createBackOffPolicy(this.getClientName());
		retryTemplate.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		RetryListener[] retryListeners = this.loadBalancedRetryListenerFactory.createRetryListeners(this.getClientName());
		if (retryListeners != null && retryListeners.length != 0) {
			retryTemplate.setListeners(retryListeners);
		}
		boolean retryable = isRequestRetryable(request);
		retryTemplate.setRetryPolicy(retryPolicy == null || !retryable ? new NeverRetryPolicy()
				: new RetryPolicy(request, retryPolicy, this, this.getClientName()));
		return retryTemplate.execute(callback, recoveryCallback);
	}

	@Override
	public OkHttpRibbonResponse execute(final OkHttpRibbonRequest ribbonRequest,
										final IClientConfig configOverride) throws Exception {
		final LoadBalancedRetryPolicy retryPolicy = loadBalancedRetryPolicyFactory.create(this.getClientName(), this);
		RetryCallback<OkHttpRibbonResponse, Exception> retryCallback  = new RetryCallback<OkHttpRibbonResponse, Exception>() {
			@Override
			public OkHttpRibbonResponse doWithRetry(RetryContext context) throws Exception {
				//on retries the policy will choose the server and set it in the context
				//extract the server and update the request being made
				OkHttpRibbonRequest newRequest = ribbonRequest;
				if(context instanceof LoadBalancedRetryContext) {
					ServiceInstance service = ((LoadBalancedRetryContext)context).getServiceInstance();
					validateServiceInstance(service);
					//Reconstruct the request URI using the host and port set in the retry context
					newRequest = newRequest.withNewUri(new URI(service.getUri().getScheme(),
							newRequest.getURI().getUserInfo(), service.getHost(), service.getPort(),
							newRequest.getURI().getPath(), newRequest.getURI().getQuery(),
							newRequest.getURI().getFragment()));
				}
				if (isSecure(configOverride)) {
					final URI secureUri = UriComponentsBuilder.fromUri(newRequest.getUri())
							.scheme("https").build().toUri();
					newRequest = newRequest.withNewUri(secureUri);
				}
				OkHttpClient httpClient = getOkHttpClient(configOverride, secure);

				final Request request = newRequest.toRequest();
				Response response = httpClient.newCall(request).execute();
				if(retryPolicy.retryableStatusCode(response.code())) {
					ResponseBody responseBody = response.peekBody(Integer.MAX_VALUE);
					response.close();
					throw new OkHttpStatusCodeException(RetryableOkHttpLoadBalancingClient.this.clientName,
							response, responseBody, newRequest.getURI());
				}
				return new OkHttpRibbonResponse(response, newRequest.getUri());
			}
		};
		return this.executeWithRetry(ribbonRequest, retryPolicy, retryCallback, new RibbonRecoveryCallback<OkHttpRibbonResponse, Response>(){

			@Override
			protected OkHttpRibbonResponse createResponse(Response response, URI uri) {
				return new OkHttpRibbonResponse(response, uri);
			}
		});
	}

	

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(OkHttpRibbonRequest request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, RetryHandler.DEFAULT, null);
	}

	static class RetryPolicy extends InterceptorRetryPolicy {
		public RetryPolicy(HttpRequest request, LoadBalancedRetryPolicy policy, ServiceInstanceChooser serviceInstanceChooser, String serviceName) {
			super(request, policy, serviceInstanceChooser, serviceName);
		}
	}
}
