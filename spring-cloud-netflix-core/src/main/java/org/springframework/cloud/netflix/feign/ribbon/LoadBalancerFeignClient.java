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

import com.netflix.client.ClientException;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;

import feign.Client;
import feign.Request;
import feign.Response;

/**
 * @author Dave Syer
 *
 */
public class LoadBalancerFeignClient implements Client {

	private final Client delegate;
	private CachingSpringLoadBalancerFactory lbClientFactory;

	public LoadBalancerFeignClient(Client delegate,
			CachingSpringLoadBalancerFactory lbClientFactory) {
		this.delegate = delegate;
		this.lbClientFactory = lbClientFactory;
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		try {
			URI asUri = URI.create(request.url());
			String clientName = asUri.getHost();
			URI uriWithoutHost = cleanUrl(request.url(), clientName);
			FeignLoadBalancer.RibbonRequest ribbonRequest = new FeignLoadBalancer.RibbonRequest(
					this.delegate, request, uriWithoutHost);
			return lbClient(clientName).executeWithLoadBalancer(ribbonRequest,
					new FeignOptionsClientConfig(options)).toResponse();
		}
		catch (ClientException e) {
			if (e.getCause() instanceof IOException) {
				throw IOException.class.cast(e.getCause());
			}
			throw new RuntimeException(e);
		}
	}

	public Client getDelegate() {
		return this.delegate;
	}

	static URI cleanUrl(String originalUrl, String host) {
		return URI.create(originalUrl.replaceFirst(host, ""));
	}

	private FeignLoadBalancer lbClient(String clientName) {
		return this.lbClientFactory.create(clientName);
	}

	static class FeignOptionsClientConfig extends DefaultClientConfigImpl {

		public FeignOptionsClientConfig(Request.Options options) {
			setProperty(CommonClientConfigKey.ConnectTimeout,
					options.connectTimeoutMillis());
			setProperty(CommonClientConfigKey.ReadTimeout, options.readTimeoutMillis());
		}

		@Override
		public void loadProperties(String clientName) {

		}

		@Override
		public void loadDefaultValues() {

		}

	}
}
