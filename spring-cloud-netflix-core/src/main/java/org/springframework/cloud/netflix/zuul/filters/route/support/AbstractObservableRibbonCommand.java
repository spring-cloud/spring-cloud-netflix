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

import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.http.client.ClientHttpResponse;

import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommandFactory.getHystrixCommandPropertiesSetter;

/**
 * @author Roman Terentiev
 */
public abstract class AbstractObservableRibbonCommand<LBC extends AbstractLoadBalancingClient<RQ, RS, ?>, RQ extends ContextAwareRequest, RS extends HttpResponse>
		extends HystrixObservableCommand<ClientHttpResponse> implements RibbonCommand {

	protected final LBC client;
	protected RibbonCommandContext context;
	protected ZuulFallbackProvider zuulFallbackProvider;
	protected IClientConfig config;

	public AbstractObservableRibbonCommand(LBC client, RibbonCommandContext context,
										   ZuulProperties zuulProperties) {
		this("default", client, context, zuulProperties);
	}

	public AbstractObservableRibbonCommand(String commandKey, LBC client,
										   RibbonCommandContext context, ZuulProperties zuulProperties) {
		this(commandKey, client, context, zuulProperties, null);
	}

	public AbstractObservableRibbonCommand(String commandKey, LBC client,
										   RibbonCommandContext context, ZuulProperties zuulProperties,
										   ZuulFallbackProvider fallbackProvider) {
		this(commandKey, client, context, zuulProperties, fallbackProvider, null);
	}

	public AbstractObservableRibbonCommand(String commandKey, LBC client,
										   RibbonCommandContext context, ZuulProperties zuulProperties,
										   ZuulFallbackProvider fallbackProvider, IClientConfig config) {
		super(getSetter(commandKey, zuulProperties));
		this.client = client;
		this.context = context;
		this.zuulFallbackProvider = fallbackProvider;
		this.config = config;
	}

	protected static Setter getSetter(final String commandKey, ZuulProperties zuulProperties) {
		return Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("RibbonCommand"))
				.andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
				.andCommandPropertiesDefaults(getHystrixCommandPropertiesSetter(commandKey, zuulProperties));
	}

	@Override
	protected Observable<ClientHttpResponse> construct() {
		return Observable.defer(new Func0<Observable<ClientHttpResponse>>() {

			@Override
			public Observable<ClientHttpResponse> call() {
				try {
					RQ request = createRequest();

					return client
							.getExecutionWithLoadBalancerObservable(request, config)
							.map(toClientHttpResponse());
				} catch (Exception e) {
					return Observable.error(e);
				}
			}
		});
	}

	@Override
	protected Observable<ClientHttpResponse> resumeWithFallback() {
		if (zuulFallbackProvider != null) {
			return Observable.just(zuulFallbackProvider.fallbackResponse());
		}

		return super.resumeWithFallback();
	}

	protected RibbonCommandContext getContext() {
		return context;
	}

	protected abstract RQ createRequest() throws Exception;

	private Func1<RS, ClientHttpResponse> toClientHttpResponse() {
		return new Func1<RS, ClientHttpResponse>() {

			@Override
			public ClientHttpResponse call(RS rs) {
				RequestContext context = RequestContext.getCurrentContext();
				context.set("ribbonResponse", rs);

				return new RibbonHttpResponse(rs);
			}
		};
	}
}
