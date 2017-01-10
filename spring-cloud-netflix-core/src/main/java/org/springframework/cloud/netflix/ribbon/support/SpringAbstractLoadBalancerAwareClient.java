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

import java.net.URI;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;

import rx.Observable;

/**
 * Override {@link com.netflix.client.AbstractLoadBalancerAwareClient#executeWithLoadBalancer(ClientRequest, IClientConfig)}
 * to support loadBalancerKey
 *
 * @author Jin Zhang
 */
public abstract class SpringAbstractLoadBalancerAwareClient<S extends ClientRequest, T extends IResponse>
		extends AbstractLoadBalancerAwareClient<S, T> {
	public SpringAbstractLoadBalancerAwareClient(ILoadBalancer lb) {
		super(lb);
	}

	public SpringAbstractLoadBalancerAwareClient(ILoadBalancer lb, IClientConfig clientConfig) {
		super(lb, clientConfig);
	}

	/**
	 * This method should be used when the caller wants to dispatch the request to a server chosen by
	 * the load balancer, instead of specifying the server in the request's URI.
	 * It calculates the final URI by calling {@link #reconstructURIWithServer(Server, URI)}
	 * and then calls {@link #executeWithLoadBalancer(ClientRequest, IClientConfig)}.
	 *
	 * @param request request to be dispatched to a server chosen by the load balancer. The URI can be a partial
	 * URI which does not contain the host name or the protocol.
	 */
	@Override
	public T executeWithLoadBalancer(final S request, final IClientConfig requestConfig) throws ClientException {
		LoadBalancerCommand<T> command = buildLoadBalancerCommand(request, requestConfig);

		try {
			return command.submit(
					new ServerOperation<T>() {
						@Override
						public Observable<T> call(Server server) {
							URI finalUri = reconstructURIWithServer(server, request.getUri());
							S requestForServer = (S) request.replaceUri(finalUri);
							try {
								return Observable.just(SpringAbstractLoadBalancerAwareClient.this.execute(requestForServer, requestConfig));
							}
							catch (Exception e) {
								return Observable.error(e);
							}
						}
					})
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

	public LoadBalancerCommand<T> buildLoadBalancerCommand(final S request, final IClientConfig requestConfig) {
		RequestSpecificRetryHandler handler = getRequestSpecificRetryHandler(request, requestConfig);
		return LoadBalancerCommand.<T>builder()
				.withLoadBalancerContext(this)
				.withRetryHandler(handler)
				.withLoadBalancerURI(request.getUri())
				.withServerLocator(request.getLoadBalancerKey()) // add load balancer key
				.build();
	}
}
