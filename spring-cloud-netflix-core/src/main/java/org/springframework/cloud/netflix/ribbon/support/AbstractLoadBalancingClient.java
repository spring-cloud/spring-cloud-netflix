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

import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;
import org.springframework.http.HttpMethod;
import rx.Observable;
import rx.Subscriber;

import java.net.URI;

import static org.apache.commons.lang.BooleanUtils.toBooleanDefaultIfNull;

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

	@Deprecated
	public AbstractLoadBalancingClient() {
		super(null);
		this.config = new DefaultClientConfigImpl();
		this.delegate = createDelegate(this.config);
		this.serverIntrospector = new DefaultServerIntrospector();
		this.setRetryHandler(RetryHandler.DEFAULT);
		initWithNiwsConfig(config);
	}

	@Deprecated
	public AbstractLoadBalancingClient(final ILoadBalancer lb) {
		super(lb);
		this.config = new DefaultClientConfigImpl();
		this.delegate = createDelegate(config);
		this.serverIntrospector = new DefaultServerIntrospector();
		this.setRetryHandler(RetryHandler.DEFAULT);
		initWithNiwsConfig(config);
	}

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
				DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
		this.readTimeout = clientConfig.getPropertyAsInteger(
				CommonClientConfigKey.ReadTimeout,
				DefaultClientConfigImpl.DEFAULT_READ_TIMEOUT);
		this.secure = clientConfig.getPropertyAsBoolean(CommonClientConfigKey.IsSecure,
				false);
		this.followRedirects = clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.FollowRedirects,
				DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS);
		this.okToRetryOnAllOperations = clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.OkToRetryOnAllOperations,
				DefaultClientConfigImpl.DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS);
	}

	@Override
	public T execute(S request, IClientConfig requestConfig) throws Exception {
		return executeInternal(request, requestConfig);
	}

	@Override
	public T executeWithLoadBalancer(S request, IClientConfig requestConfig) throws ClientException {
		try {
			return getExecutionWithLoadBalancerObservable(request, requestConfig)
					.toBlocking()
					.single();
		} catch (Exception e) {
			Throwable t = e.getCause();
			if (t instanceof ClientException) {
				throw (ClientException) t;
			} else {
				throw new ClientException(e);
			}
		}
	}

	public Observable<T> getExecutionWithLoadBalancerObservable(S request, IClientConfig requestConfig) {
		RequestSpecificRetryHandler handler = getRequestSpecificRetryHandler(request, config);

		LoadBalancerCommand<T> command = LoadBalancerCommand.<T>builder()
				.withLoadBalancerContext(this)
				.withRetryHandler(handler)
				.withLoadBalancerURI(request.getURI())
				.build();

		try {
			return command.submit(getServerOperation(request, requestConfig));
		} catch (Exception e) {
			return Observable.error(e);
		}
	}

	protected abstract T executeInternal(S request, IClientConfig requestConfig) throws Exception;

	protected abstract D createDelegate(IClientConfig config);

	public D getDelegate() {
		return this.delegate;
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(S request, IClientConfig requestConfig) {
		boolean retryable = request.getContext() == null
				|| toBooleanDefaultIfNull(request.getContext().getRetryable(), true);

		if (retryable) {
			boolean okToRetryOnAllErrors = okToRetryOnAllOperations || request.getMethod() == HttpMethod.GET;
			RetryHandler retryHandler = getRetryHandler();

			return new RequestSpecificRetryHandler(false, okToRetryOnAllErrors, retryHandler, requestConfig);
		}

		return new RequestSpecificRetryHandler(false, false, RetryHandler.DEFAULT, null);
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

	protected ServerOperation<T> getServerOperation(final S request, final IClientConfig requestConfig) {
		return new ServerOperation<T>() {

			@Override
			public Observable<T> call(final Server server) {
				return Observable.create(new Observable.OnSubscribe<T>() {

					@Override
					public void call(Subscriber<? super T> subscriber) {
						URI finalUri = reconstructURIWithServer(server, request.getURI());
						S requestForServer = (S) request.replaceUri(finalUri);

						try {
							T response = AbstractLoadBalancingClient.this.executeInternal(requestForServer, requestConfig);
							if (!subscriber.isUnsubscribed()) {
								subscriber.onNext(response);
								subscriber.onCompleted();
							} else {
								response.close();
							}
						} catch (Exception e) {
							if (!subscriber.isUnsubscribed()) {
								subscriber.onError(e);
							}
						}
					}
				});
			}
		};
	}
}
