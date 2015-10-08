/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.netflix.feign.ribbon;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;

public class FeignLoadBalancer extends
		AbstractLoadBalancerAwareClient<FeignLoadBalancer.RibbonRequest, FeignLoadBalancer.RibbonResponse> {

	private final int connectTimeout;
	private final int readTimeout;
	private final IClientConfig clientConfig;
	private final ServerIntrospector serverIntrospector;

	public FeignLoadBalancer(ILoadBalancer lb, IClientConfig clientConfig,
			ServerIntrospector serverIntrospector) {
		super(lb, clientConfig);
		this.setRetryHandler(RetryHandler.DEFAULT);
		this.clientConfig = clientConfig;
		this.connectTimeout = clientConfig.get(CommonClientConfigKey.ConnectTimeout);
		this.readTimeout = clientConfig.get(CommonClientConfigKey.ReadTimeout);
		this.serverIntrospector = serverIntrospector;
	}

	@Override
	public RibbonResponse execute(RibbonRequest request, IClientConfig configOverride)
			throws IOException {
		Request.Options options;
		if (configOverride != null) {
			options = new Request.Options(
					configOverride.get(CommonClientConfigKey.ConnectTimeout,
							this.connectTimeout),
					(configOverride.get(CommonClientConfigKey.ReadTimeout,
							this.readTimeout)));
		}
		else {
			options = new Request.Options(this.connectTimeout, this.readTimeout);
		}
		Response response = request.client().execute(request.toRequest(), options);
		return new RibbonResponse(request.getUri(), response);
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			RibbonRequest request, IClientConfig requestConfig) {
		if (this.clientConfig.get(CommonClientConfigKey.OkToRetryOnAllOperations,
				false)) {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(),
					requestConfig);
		}
		if (!request.toRequest().method().equals("GET")) {
			return new RequestSpecificRetryHandler(true, false, this.getRetryHandler(),
					requestConfig);
		}
		else {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(),
					requestConfig);
		}
	}

	@Override
	public URI reconstructURIWithServer(Server server, URI original) {
		String scheme = original.getScheme();
		if (!"https".equals(scheme) && (this.serverIntrospector.isSecure(server)
				|| this.clientConfig.get(CommonClientConfigKey.IsSecure, false))) {
			original = UriComponentsBuilder.fromUri(original).scheme("https").build()
					.toUri();
		}
		return super.reconstructURIWithServer(server, original);
	}

	static class RibbonRequest extends ClientRequest implements Cloneable {

		private final Request request;
		private final Client client;

		RibbonRequest(Client client, Request request, URI uri) {
			this.client = client;
			this.request = request;
			setUri(uri);
		}

		Request toRequest() {
			return new RequestTemplate().method(this.request.method())
					.append(getUri().toASCIIString()).headers(this.request.headers())
					.body(this.request.body(), this.request.charset()).request();
		}

		Client client() {
			return this.client;
		}

		@Override
		public Object clone() {
			return new RibbonRequest(this.client, this.request, getUri());
		}
	}

	static class RibbonResponse implements IResponse {

		private final URI uri;
		private final Response response;

		RibbonResponse(URI uri, Response response) {
			this.uri = uri;
			this.response = response;
		}

		@Override
		public Object getPayload() throws ClientException {
			return this.response.body();
		}

		@Override
		public boolean hasPayload() {
			return this.response.body() != null;
		}

		@Override
		public boolean isSuccess() {
			return this.response.status() == 200;
		}

		@Override
		public URI getRequestedURI() {
			return this.uri;
		}

		@Override
		public Map<String, Collection<String>> getHeaders() {
			return this.response.headers();
		}

		Response toResponse() {
			return this.response;
		}

		@Override
		public void close() throws IOException {
			if (this.response != null && this.response.body() != null) {
				this.response.body().close();
			}
		}

	}

}