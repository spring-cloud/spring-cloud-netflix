/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.ribbon.support;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.RibbonProperties;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;

import static org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration.DEFAULT_CONNECT_TIMEOUT;
import static org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration.DEFAULT_GZIP_PAYLOAD;
import static org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration.DEFAULT_READ_TIMEOUT;

/**
 * @param <D> delegate
 * @param <S> {@link ContextAwareRequest} subtype
 * @param <T> {@link ServiceInstanceChooser} subtype
 * @author Spencer Gibb
 *
 */
public abstract class AbstractLoadBalancingClient<S extends ContextAwareRequest, T extends IResponse, D>
		extends AbstractLoadBalancerAwareClient<S, T> implements ServiceInstanceChooser {

	protected int connectTimeout;

	protected int readTimeout;

	protected boolean secure;

	protected boolean followRedirects;

	protected boolean gzipPayload;

	protected boolean okToRetryOnAllOperations;

	protected final D delegate;

	protected final IClientConfig config;

	protected final ServerIntrospector serverIntrospector;

	public boolean isClientRetryable(ContextAwareRequest request) {
		return false;
	}

	protected AbstractLoadBalancingClient(IClientConfig config,
			ServerIntrospector serverIntrospector) {
		super(null);
		this.delegate = createDelegate(config);
		this.config = config;
		this.serverIntrospector = serverIntrospector;
		this.setRetryHandler(RetryHandler.DEFAULT);
		initWithNiwsConfig(config);
	}

	protected AbstractLoadBalancingClient(D delegate, IClientConfig config,
			ServerIntrospector serverIntrospector) {
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
		RibbonProperties ribbon = RibbonProperties.from(clientConfig);
		this.connectTimeout = ribbon.connectTimeout(DEFAULT_CONNECT_TIMEOUT);
		this.readTimeout = ribbon.readTimeout(DEFAULT_READ_TIMEOUT);
		this.secure = ribbon.isSecure();
		this.followRedirects = ribbon.isFollowRedirects();
		this.okToRetryOnAllOperations = ribbon.isOkToRetryOnAllOperations();
		this.gzipPayload = ribbon.isGZipPayload(DEFAULT_GZIP_PAYLOAD);
	}

	protected abstract D createDelegate(IClientConfig config);

	public D getDelegate() {
		return this.delegate;
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(final S request,
			final IClientConfig requestConfig) {
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
		if (config != null) {
			return RibbonProperties.from(config).isSecure(this.secure);
		}
		return this.secure;
	}

	@Override
	protected void customizeLoadBalancerCommandBuilder(S request, IClientConfig config,
			LoadBalancerCommand.Builder<T> builder) {
		if (request.getLoadBalancerKey() != null) {
			builder.withServerLocator(request.getLoadBalancerKey());
		}
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		Server server = this.getLoadBalancer().chooseServer(serviceId);
		if (server != null) {
			return new RibbonLoadBalancerClient.RibbonServer(serviceId, server);
		}
		return null;
	}

	public void validateServiceInstance(ServiceInstance serviceInstance)
			throws ClientException {
		if (serviceInstance == null) {
			throw new ClientException(
					"Load balancer does not have available server for client: "
							+ clientName);
		}
		else if (serviceInstance.getHost() == null) {
			throw new ClientException("Invalid Server for: "
					+ serviceInstance.getServiceId() + " null Host");
		}
	}

}
