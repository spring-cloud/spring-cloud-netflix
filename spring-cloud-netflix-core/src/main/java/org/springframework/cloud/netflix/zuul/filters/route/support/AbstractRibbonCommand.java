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

package org.springframework.cloud.netflix.zuul.filters.route.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.client.ClientHttpResponse;
import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientRequest;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.RequestContext;

/**
 * @author Spencer Gibb
 */
public abstract class AbstractRibbonCommand<LBC extends AbstractLoadBalancerAwareClient<RQ, RS>, RQ extends ClientRequest, RS extends HttpResponse>
		extends HystrixCommand<ClientHttpResponse> implements RibbonCommand {

	private static final Log LOGGER = LogFactory.getLog(AbstractRibbonCommand.class);
	protected final LBC client;
	protected RibbonCommandContext context;
	protected ZuulFallbackProvider zuulFallbackProvider;
	protected IClientConfig config;

	public AbstractRibbonCommand(LBC client, RibbonCommandContext context,
			ZuulProperties zuulProperties) {
		this("default", client, context, zuulProperties);
	}

	public AbstractRibbonCommand(String commandKey, LBC client,
			RibbonCommandContext context, ZuulProperties zuulProperties) {
		this(commandKey, client, context, zuulProperties, null);
	}

	public AbstractRibbonCommand(String commandKey, LBC client,
								 RibbonCommandContext context, ZuulProperties zuulProperties,
								 ZuulFallbackProvider fallbackProvider) {
		this(commandKey, client, context, zuulProperties, fallbackProvider, null);
	}

	public AbstractRibbonCommand(String commandKey, LBC client,
								 RibbonCommandContext context, ZuulProperties zuulProperties,
								 ZuulFallbackProvider fallbackProvider, IClientConfig config) {
		this(getSetter(commandKey, zuulProperties, config), client, context, fallbackProvider, config);
	}

	protected AbstractRibbonCommand(Setter setter, LBC client,
								 RibbonCommandContext context,
								 ZuulFallbackProvider fallbackProvider, IClientConfig config) {
		super(setter);
		this.client = client;
		this.context = context;
		this.zuulFallbackProvider = fallbackProvider;
		this.config = config;
	}

	protected static HystrixCommandProperties.Setter createSetter(IClientConfig config, String commandKey, ZuulProperties zuulProperties) {
		int hystrixTimeout = getHystrixTimeout(config, commandKey);
		return HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
				zuulProperties.getRibbonIsolationStrategy()).withExecutionTimeoutInMilliseconds(hystrixTimeout);
	}

	protected static int getHystrixTimeout(IClientConfig config, String commandKey) {
		int ribbonTimeout = getRibbonTimeout(config, commandKey);
		DynamicPropertyFactory dynamicPropertyFactory = DynamicPropertyFactory.getInstance();
		int defaultHystrixTimeout = dynamicPropertyFactory.getIntProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",
			0).get();
		int commandHystrixTimeout = dynamicPropertyFactory.getIntProperty("hystrix.command." + commandKey + ".execution.isolation.thread.timeoutInMilliseconds",
			0).get();
		int hystrixTimeout;
		if(commandHystrixTimeout > 0) {
			hystrixTimeout = commandHystrixTimeout;
		}
		else if(defaultHystrixTimeout > 0) {
			hystrixTimeout = defaultHystrixTimeout;
		} else {
			hystrixTimeout = ribbonTimeout;
		}
		if(hystrixTimeout < ribbonTimeout) {
			LOGGER.warn("The Hystrix timeout of " + hystrixTimeout + "ms for the command " + commandKey +
				" is set lower than the combination of the Ribbon read and connect timeout, " + ribbonTimeout + "ms.");
		}
		return hystrixTimeout;
	}

	protected static int getRibbonTimeout(IClientConfig config, String commandKey) {
		int ribbonTimeout;
		if (config == null) {
			ribbonTimeout = RibbonClientConfiguration.DEFAULT_READ_TIMEOUT + RibbonClientConfiguration.DEFAULT_CONNECT_TIMEOUT;
		} else {
			int ribbonReadTimeout = getTimeout(config, commandKey, "ReadTimeout",
				IClientConfigKey.Keys.ReadTimeout, RibbonClientConfiguration.DEFAULT_READ_TIMEOUT);
			int ribbonConnectTimeout = getTimeout(config, commandKey, "ConnectTimeout",
				IClientConfigKey.Keys.ConnectTimeout, RibbonClientConfiguration.DEFAULT_CONNECT_TIMEOUT);
			int maxAutoRetries = getTimeout(config, commandKey, "MaxAutoRetries",
				IClientConfigKey.Keys.MaxAutoRetries, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES);
			int maxAutoRetriesNextServer = getTimeout(config, commandKey, "MaxAutoRetriesNextServer",
				IClientConfigKey.Keys.MaxAutoRetriesNextServer, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER);
			ribbonTimeout = (ribbonReadTimeout + ribbonConnectTimeout) * (maxAutoRetries + 1) * (maxAutoRetriesNextServer + 1);
		}
		return ribbonTimeout;
	}

	private static int getTimeout(IClientConfig config, String commandKey, String property, IClientConfigKey<Integer> configKey, int defaultValue) {
		DynamicPropertyFactory dynamicPropertyFactory = DynamicPropertyFactory.getInstance();
		return dynamicPropertyFactory.getIntProperty(commandKey + "." + config.getNameSpace() + "." + property, config.get(configKey, defaultValue)).get();
	}

	@Deprecated
	//TODO remove in 2.0.x
	protected static Setter getSetter(final String commandKey, ZuulProperties zuulProperties) {
		return getSetter(commandKey, zuulProperties, null);
	}

	protected static Setter getSetter(final String commandKey,
			ZuulProperties zuulProperties, IClientConfig config) {

		// @formatter:off
		Setter commandSetter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RibbonCommand"))
								.andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
		final HystrixCommandProperties.Setter setter = createSetter(config, commandKey, zuulProperties);
		if (zuulProperties.getRibbonIsolationStrategy() == ExecutionIsolationStrategy.SEMAPHORE){
			final String name = ZuulConstants.ZUUL_EUREKA + commandKey + ".semaphore.maxSemaphores";
			// we want to default to semaphore-isolation since this wraps
			// 2 others commands that are already thread isolated
			final DynamicIntProperty value = DynamicPropertyFactory.getInstance()
					.getIntProperty(name, zuulProperties.getSemaphore().getMaxSemaphores());
			setter.withExecutionIsolationSemaphoreMaxConcurrentRequests(value.get());
		} else if (zuulProperties.getThreadPool().isUseSeparateThreadPools()) {
			final String threadPoolKey = zuulProperties.getThreadPool().getThreadPoolKeyPrefix() + commandKey;
			commandSetter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolKey));
		}
		
		return commandSetter.andCommandPropertiesDefaults(setter);
		// @formatter:on
	}

	@Override
	protected ClientHttpResponse run() throws Exception {
		final RequestContext context = RequestContext.getCurrentContext();

		RQ request = createRequest();
		RS response;
		
		boolean retryableClient = this.client instanceof AbstractLoadBalancingClient
				&& ((AbstractLoadBalancingClient)this.client).isClientRetryable((ContextAwareRequest)request);
		
		if (retryableClient) {
			response = this.client.execute(request, config);
		} else {
			response = this.client.executeWithLoadBalancer(request, config);
		}
		context.set("ribbonResponse", response);

		// Explicitly close the HttpResponse if the Hystrix command timed out to
		// release the underlying HTTP connection held by the response.
		//
		if (this.isResponseTimedOut()) {
			if (response != null) {
				response.close();
			}
		}

		return new RibbonHttpResponse(response);
	}

	@Override
	protected ClientHttpResponse getFallback() {
		if(zuulFallbackProvider != null) {
			return getFallbackResponse();
		}
		return super.getFallback();
	}

	protected ClientHttpResponse getFallbackResponse() {
		if (zuulFallbackProvider instanceof FallbackProvider) {
			Throwable cause = getFailedExecutionException();
			cause = cause == null ? getExecutionException() : cause;
			if (cause == null) {
				zuulFallbackProvider.fallbackResponse();
			} else {
				return ((FallbackProvider) zuulFallbackProvider).fallbackResponse(cause);
			}
		}
		return zuulFallbackProvider.fallbackResponse();
	}

	public LBC getClient() {
		return client;
	}

	public RibbonCommandContext getContext() {
		return context;
	}

	protected abstract RQ createRequest() throws Exception;
}
