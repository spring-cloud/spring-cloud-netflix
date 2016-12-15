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

import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.netflix.loadbalancer.reactive.ServerOperation;
import com.netflix.niws.client.http.RestClient;
import com.sun.jersey.api.client.Client;

import rx.Observable;

/**
 * Override {@link com.netflix.client.AbstractLoadBalancerAwareClient#executeWithLoadBalancer(ClientRequest, IClientConfig)}
 * to support loadBalancerKey
 *
 * @author Jin Zhang
 */
@Deprecated
public class SpringRestClient extends RestClient {
	public SpringRestClient() {
		super();
	}

	public SpringRestClient(ILoadBalancer lb) {
		super(lb);
	}

	public SpringRestClient(ILoadBalancer lb, IClientConfig ncc) {
		super(lb, ncc);
	}

	public SpringRestClient(IClientConfig ncc) {
		super(ncc);
	}

	public SpringRestClient(ILoadBalancer lb, Client jerseyClient) {
		super(lb, jerseyClient);
	}

	@Override
	public HttpResponse executeWithLoadBalancer(final HttpRequest request, final IClientConfig requestConfig) throws ClientException {
		LoadBalancerCommand<HttpResponse> command = buildLoadBalancerCommand(request, requestConfig);

		try {
			return command.submit(
					new ServerOperation<HttpResponse>() {
						@Override
						public Observable<HttpResponse> call(Server server) {
							URI finalUri = reconstructURIWithServer(server, request.getUri());
							HttpRequest requestForServer = request.replaceUri(finalUri);
							try {
								return Observable.just(SpringRestClient.this.execute(requestForServer, requestConfig));
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

	public LoadBalancerCommand<HttpResponse> buildLoadBalancerCommand(final HttpRequest request, final IClientConfig requestConfig) {
		RequestSpecificRetryHandler handler = getRequestSpecificRetryHandler(request, requestConfig);
		return LoadBalancerCommand.<HttpResponse>builder()
				.withLoadBalancerContext(this)
				.withRetryHandler(handler)
				.withLoadBalancerURI(request.getUri())
				.withServerLocator(request.getLoadBalancerKey()) // add load balancer key
				.build();
	}
}
