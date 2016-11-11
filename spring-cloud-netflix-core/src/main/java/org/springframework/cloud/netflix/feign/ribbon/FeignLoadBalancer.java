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

import feign.Client;
import feign.Request;
import feign.Response;
import feign.Util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientException;
import com.netflix.client.ClientRequest;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.IResponse;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToHttpsIfNeeded;

public class FeignLoadBalancer extends
		AbstractLoadBalancerAwareClient<FeignLoadBalancer.RibbonRequest, FeignLoadBalancer.RibbonResponse> implements
		ServiceInstanceChooser {

	private final int connectTimeout;
	private final int readTimeout;
	private final IClientConfig clientConfig;
	private final ServerIntrospector serverIntrospector;
	private final RetryTemplate retryTemplate;
	private final LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;

	public FeignLoadBalancer(ILoadBalancer lb, IClientConfig clientConfig,
			ServerIntrospector serverIntrospector, RetryTemplate retryTemplate,
							 LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(lb, clientConfig);
		this.retryTemplate = retryTemplate;
		this.loadBalancedRetryPolicyFactory = loadBalancedRetryPolicyFactory;
		this.setRetryHandler(new DefaultLoadBalancerRetryHandler(clientConfig));
		this.clientConfig = clientConfig;
		this.connectTimeout = clientConfig.get(CommonClientConfigKey.ConnectTimeout);
		this.readTimeout = clientConfig.get(CommonClientConfigKey.ReadTimeout);
		this.serverIntrospector = serverIntrospector;
	}

	@Override
	public RibbonResponse execute(final RibbonRequest request, IClientConfig configOverride)
			throws IOException {
		final Request.Options options;
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
		LoadBalancedRetryPolicy retryPolicy = loadBalancedRetryPolicyFactory.create(this.getClientName(), this);
		retryTemplate.setRetryPolicy(retryPolicy == null ? new NeverRetryPolicy()
						: new FeignRetryPolicy(request.toHttpRequest(), retryPolicy, this, this.getClientName()));
		return retryTemplate.execute(new RetryCallback<RibbonResponse, IOException>() {
			@Override
			public RibbonResponse doWithRetry(RetryContext retryContext) throws IOException {
				Request feignRequest = null;
				//on retries the policy will choose the server and set it in the context
				//extract the server and update the request being made
				if(retryContext instanceof LoadBalancedRetryContext) {
					ServiceInstance service = ((LoadBalancedRetryContext)retryContext).getServiceInstance();
					if(service != null) {
						feignRequest = ((RibbonRequest)request.replaceUri(reconstructURIWithServer(new Server(service.getHost(), service.getPort()), request.getUri()))).toRequest();
					}
				}
				if(feignRequest == null) {
					feignRequest = request.toRequest();
				}
				Response response = request.client().execute(feignRequest, options);
				return new RibbonResponse(request.getUri(), response);
			}
		});
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			RibbonRequest request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, this.getRetryHandler(), requestConfig);
	}

	@Override
	public URI reconstructURIWithServer(Server server, URI original) {
		URI uri = updateToHttpsIfNeeded(original, this.clientConfig, this.serverIntrospector, server);
		return super.reconstructURIWithServer(server, uri);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return new RibbonLoadBalancerClient.RibbonServer(serviceId,
				this.getLoadBalancer().chooseServer(serviceId));
	}

	static class RibbonRequest extends ClientRequest implements Cloneable {

		private final Request request;
		private final Client client;

		RibbonRequest(Client client, Request request, URI uri) {
			this.client = client;
			setUri(uri);
			this.request = toRequest(request);
		}

		private Request toRequest(Request request) {
			Map<String, Collection<String>> headers = new LinkedHashMap<>(
					request.headers());
			// Apache client barfs if you set the content length
			headers.remove(Util.CONTENT_LENGTH);
			return Request.create(request.method(),getUri().toASCIIString(),headers,request.body(),request.charset());
		}

		Request toRequest() {
			return toRequest(this.request);
		}

		Client client() {
			return this.client;
		}

		HttpRequest toHttpRequest() {
			return new HttpRequest() {
				@Override
				public HttpMethod getMethod() {
					return HttpMethod.resolve(RibbonRequest.this.toRequest().method());
				}

				@Override
				public URI getURI() {
					return RibbonRequest.this.getUri();
				}

				@Override
				public HttpHeaders getHeaders() {
					Map<String, List<String>> headers = new HashMap<String, List<String>>();
					Map<String, Collection<String>> feignHeaders = RibbonRequest.this.toRequest().headers();
					for(String key : feignHeaders.keySet()) {
						headers.put(key, new ArrayList<String>(feignHeaders.get(key)));
					}
					HttpHeaders httpHeaders = new HttpHeaders();
					httpHeaders.putAll(headers);
					return httpHeaders;

				}
			};
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
