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

package org.springframework.cloud.netflix.ribbon.apache;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

import lombok.Setter;

/**
 * @author Christian Lohmann
 */
public class RibbonLoadBalancingHttpClient
	extends AbstractLoadBalancerAwareClient<RibbonApacheHttpRequest, RibbonApacheHttpResponse> {
	private final HttpClient delegate = HttpClientBuilder.create().build();

	@Setter
	private int connectTimeout;

	@Setter
	private int readTimeout;

	@Setter
	private boolean secure;

	@Setter
	private IClientConfig clientConfig;

	public RibbonLoadBalancingHttpClient() {
		super(null);
		this.setRetryHandler(RetryHandler.DEFAULT);
	}

	public RibbonLoadBalancingHttpClient(final ILoadBalancer lb) {
		super(lb);
		this.setRetryHandler(RetryHandler.DEFAULT);
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(final RibbonApacheHttpRequest request,
		final IClientConfig requestConfig) {
		if (this.clientConfig.get(CommonClientConfigKey.OkToRetryOnAllOperations, false)) {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(), requestConfig);
		}

		if (!request.getMethod().equals("GET")) {
			return new RequestSpecificRetryHandler(true, false, this.getRetryHandler(), requestConfig);
		}
		else {
			return new RequestSpecificRetryHandler(true, true, this.getRetryHandler(), requestConfig);
		}
	}

	@Override
	public RibbonApacheHttpResponse execute(RibbonApacheHttpRequest request, final IClientConfig configOverride)
		throws Exception {
		final RequestConfig.Builder builder = RequestConfig.custom();
		if (configOverride != null) {
			builder.setConnectTimeout(configOverride.get(CommonClientConfigKey.ConnectTimeout, this.connectTimeout));
			builder
				.setConnectionRequestTimeout(configOverride.get(CommonClientConfigKey.ReadTimeout, this.readTimeout));
		}
		else {
			builder.setConnectTimeout(this.connectTimeout);
			builder.setConnectionRequestTimeout(this.readTimeout);
		}

		final RequestConfig requestConfig = builder.build();

		if (isSecure(configOverride)) {
			final URI secureUri = UriComponentsBuilder.fromUri(request.getUri()).scheme("https").build().toUri();
			request = request.withNewUri(secureUri);
		}

		final HttpUriRequest httpUriRequest = request.toRequest(requestConfig);
		final HttpResponse httpResponse = this.delegate.execute(httpUriRequest);
		return new RibbonApacheHttpResponse(httpResponse, httpUriRequest.getURI());
	}

	private boolean isSecure(final IClientConfig config) {
		return (config != null) ? config.get(CommonClientConfigKey.IsSecure) : secure;
	}
}
