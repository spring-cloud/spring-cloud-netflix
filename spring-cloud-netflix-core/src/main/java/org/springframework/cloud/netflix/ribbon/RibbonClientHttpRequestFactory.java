/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.niws.client.http.RestClient;

/**
 * @author Spencer Gibb
 */
public class RibbonClientHttpRequestFactory implements ClientHttpRequestFactory {

	private final SpringClientFactory clientFactory;

    private LoadBalancerClient loadBalancer;

	public RibbonClientHttpRequestFactory(SpringClientFactory clientFactory, LoadBalancerClient loadBalancer) {
		this.clientFactory = clientFactory;
        this.loadBalancer = loadBalancer;
	}

	@Override
	@SuppressWarnings("deprecation")
	public ClientHttpRequest createRequest(URI originalUri, HttpMethod httpMethod)
			throws IOException {
        String serviceId = originalUri.getHost();
        ServiceInstance instance = loadBalancer.choose(serviceId);
        URI uri = loadBalancer.reconstructURI(instance, originalUri);
        //@formatter:off
		IClientConfig clientConfig = clientFactory.getClientConfig(instance.getServiceId());
		RestClient client = clientFactory.getClient(instance.getServiceId(), RestClient.class);
		HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .verb(HttpRequest.Verb.valueOf(httpMethod.name()))
                .build();
        //@formatter:on
		return new RibbonHttpRequest(request, client, clientConfig);
	}

	public class RibbonHttpRequest extends AbstractClientHttpRequest {

		private HttpRequest request;
		private RestClient client;
		private IClientConfig config;

		@SuppressWarnings("deprecation")
		public RibbonHttpRequest(HttpRequest request, RestClient client,
				IClientConfig config) {
			this.request = request;
			this.client = client;
			this.config = config;
			request.getHeaders().putAll(getHeaders());
		}

		@Override
		public HttpMethod getMethod() {
			return HttpMethod.valueOf(request.getVerb().name());
		}

		@Override
		public URI getURI() {
			return request.getUri();
		}

		@Override
		protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
			throw new RuntimeException("Not implemented");
		}

		@Override
		protected ClientHttpResponse executeInternal(HttpHeaders headers)
				throws IOException {
            // use execute here so stats are collected
            return loadBalancer.execute(this.config.getClientName(), new LoadBalancerRequest<ClientHttpResponse>() {
                @Override
                public ClientHttpResponse apply(ServiceInstance instance) throws Exception {
                    HttpResponse response = client.execute(request, config);
                    return new RibbonHttpResponse(response);
                }
            });
		}
	}

	public class RibbonHttpResponse extends AbstractClientHttpResponse {

		private HttpResponse response;
		private HttpHeaders httpHeaders;

		public RibbonHttpResponse(HttpResponse response) {
			this.response = response;
			this.httpHeaders = new HttpHeaders();
			List<Map.Entry<String, String>> headers = response.getHttpHeaders()
					.getAllHeaders();
			for (Map.Entry<String, String> header : headers) {
				this.httpHeaders.add(header.getKey(), header.getValue());
			}
		}

		@Override
		public InputStream getBody() throws IOException {
			return response.getInputStream();
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.httpHeaders;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.getStatus();
		}

		@Override
		public String getStatusText() throws IOException {
			return HttpStatus.valueOf(response.getStatus()).name();
		}

		@Override
		public void close() {
			response.close();
		}

	}
}
