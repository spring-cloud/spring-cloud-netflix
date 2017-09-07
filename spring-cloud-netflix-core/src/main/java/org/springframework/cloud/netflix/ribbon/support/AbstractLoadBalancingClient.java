/*
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

import org.springframework.cloud.netflix.ribbon.RibbonClientConfigDefaults;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;

/**
 * @author Spencer Gibb
 */
public abstract class AbstractLoadBalancingClient<S extends ContextAwareRequest, T extends IResponse, D> extends
		AbstractLoadBalancerAwareClient<S, T> {

	protected int connectTimeout;

	protected int readTimeout;

	protected boolean secure;

	protected boolean followRedirects;

	protected boolean okToRetryOnAllOperations;

	protected final D delegate;
	protected final IClientConfig config;
	protected final ServerIntrospector serverIntrospector;

	protected AbstractLoadBalancingClient(IClientConfig config, ServerIntrospector serverIntrospector) {
		super(null);
		this.delegate = createDelegate(config);
		this.config = config;
		this.serverIntrospector = serverIntrospector;
		this.setRetryHandler(RetryHandler.DEFAULT);
		initWithNiwsConfig(config);
	}

	protected AbstractLoadBalancingClient(D delegate, IClientConfig config, ServerIntrospector serverIntrospector) {
		super(null);
		this.delegate = delegate;
		this.config = config;
		this.serverIntrospector = serverIntrospector;
		this.setRetryHandler(RetryHandler.DEFAULT);
		initWithNiwsConfig(config);
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		super.initWithNiwsConfig(clientConfig);
		this.connectTimeout = clientConfig.getPropertyAsInteger(
				CommonClientConfigKey.ConnectTimeout,
				RibbonClientConfigDefaults.DEFAULT_CONNECT_TIMEOUT);
		this.readTimeout = clientConfig.getPropertyAsInteger(
				CommonClientConfigKey.ReadTimeout,
				RibbonClientConfigDefaults.DEFAULT_READ_TIMEOUT);
		this.secure = clientConfig.getPropertyAsBoolean(CommonClientConfigKey.IsSecure,
				false);
		this.followRedirects = clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.FollowRedirects,
				RibbonClientConfigDefaults.DEFAULT_FOLLOW_REDIRECTS);
		this.okToRetryOnAllOperations = clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.OkToRetryOnAllOperations,
				RibbonClientConfigDefaults.DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS);
	}

	protected abstract D createDelegate(IClientConfig config);

	public D getDelegate() {
		return this.delegate;
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			final S request, final IClientConfig requestConfig) {
		if (this.okToRetryOnAllOperations) {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(),
					requestConfig);
		}

		if (!request.getContext().getMethod().equals("GET")) {
			return new RequestSpecificRetryHandler(true, false, this.getRetryHandler(),
					requestConfig);
		}
		else {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(),
					requestConfig);
		}
	}

	protected boolean isSecure(final IClientConfig config) {
		if(config != null) {
			Boolean result = config.get(CommonClientConfigKey.IsSecure);
			if(result != null) {
				return result;
			}
		}
		return this.secure;
	}
}
