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

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * @author Christian Lohmann
 */
public class RibbonLoadBalancingHttpClient
		extends
		AbstractLoadBalancingClient<RibbonApacheHttpRequest, RibbonApacheHttpResponse> {
	private final HttpClient delegate = HttpClientBuilder.create().disableCookieManagement().build();

	public RibbonLoadBalancingHttpClient() {
		super();
	}

	public RibbonLoadBalancingHttpClient(final ILoadBalancer lb) {
		super(lb);
	}

	@Override
	public RibbonApacheHttpResponse execute(RibbonApacheHttpRequest request,
			final IClientConfig configOverride) throws Exception {
		final RequestConfig.Builder builder = RequestConfig.custom();
		if (configOverride != null) {
			builder.setConnectTimeout(configOverride.get(
					CommonClientConfigKey.ConnectTimeout, this.connectTimeout));
			builder.setSocketTimeout(configOverride.get(
					CommonClientConfigKey.ReadTimeout, this.readTimeout));
			builder.setRedirectsEnabled(configOverride.get(
					CommonClientConfigKey.FollowRedirects, this.followRedirects));
		}
		else {
			builder.setConnectTimeout(this.connectTimeout);
			builder.setSocketTimeout(this.readTimeout);
			builder.setRedirectsEnabled(this.followRedirects);
		}

		final RequestConfig requestConfig = builder.build();

		if (isSecure(configOverride)) {
			final URI secureUri = UriComponentsBuilder.fromUri(request.getUri())
					.scheme("https").build().toUri();
			request = request.withNewUri(secureUri);
		}

		final HttpUriRequest httpUriRequest = request.toRequest(requestConfig);
		final HttpResponse httpResponse = this.delegate.execute(httpUriRequest);
		return new RibbonApacheHttpResponse(httpResponse, httpUriRequest.getURI());
	}

}
