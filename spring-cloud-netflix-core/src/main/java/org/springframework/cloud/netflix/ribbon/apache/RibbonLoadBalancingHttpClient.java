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

import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToHttpsIfNeeded;

/**
 * @author Christian Lohmann
 * @author Ryan Baxter
 */
// TODO: rename (ie new class that extends this in Dalston) to ApacheHttpLoadBalancingClient
public class RibbonLoadBalancingHttpClient extends
		AbstractLoadBalancingClient<RibbonApacheHttpRequest, RibbonApacheHttpResponse, CloseableHttpClient> {

	public RibbonLoadBalancingHttpClient(IClientConfig config,
			ServerIntrospector serverIntrospector) {
		super(config, serverIntrospector);
	}

	public RibbonLoadBalancingHttpClient(CloseableHttpClient delegate,
			IClientConfig config, ServerIntrospector serverIntrospector) {
		super(delegate, config, serverIntrospector);
	}

	protected CloseableHttpClient createDelegate(IClientConfig config) {
		return HttpClientBuilder.create()
				// already defaults to 0 in builder, so resetting to 0 won't hurt
				.setMaxConnTotal(config.getPropertyAsInteger(
						CommonClientConfigKey.MaxTotalConnections, 0))
				// already defaults to 0 in builder, so resetting to 0 won't hurt
				.setMaxConnPerRoute(config.getPropertyAsInteger(
						CommonClientConfigKey.MaxConnectionsPerHost, 0))
				.disableCookieManagement().useSystemProperties() // for proxy
				.build();
	}

	@Override
	public RibbonApacheHttpResponse execute(RibbonApacheHttpRequest request,
			final IClientConfig configOverride) throws Exception {
		final RequestConfig.Builder builder = RequestConfig.custom();
		IClientConfig config = configOverride != null ? configOverride : this.config;
		builder.setConnectTimeout(
				config.get(CommonClientConfigKey.ConnectTimeout, this.connectTimeout));
		builder.setSocketTimeout(
				config.get(CommonClientConfigKey.ReadTimeout, this.readTimeout));
		builder.setRedirectsEnabled(
				config.get(CommonClientConfigKey.FollowRedirects, this.followRedirects));

		final RequestConfig requestConfig = builder.build();
		request = getSecureRequest(request, configOverride);
		final HttpUriRequest httpUriRequest = request.toRequest(requestConfig);
		final HttpResponse httpResponse = this.delegate.execute(httpUriRequest);
		return new RibbonApacheHttpResponse(httpResponse, httpUriRequest.getURI());
	}

	@Override
	public URI reconstructURIWithServer(Server server, URI original) {
		URI uri = updateToHttpsIfNeeded(original, this.config, this.serverIntrospector,
				server);
		return super.reconstructURIWithServer(server, uri);
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			RibbonApacheHttpRequest request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, RetryHandler.DEFAULT,
				requestConfig);
	}

	protected RibbonApacheHttpRequest getSecureRequest(RibbonApacheHttpRequest request, IClientConfig configOverride) {
		if (isSecure(configOverride)) {
			final URI secureUri = UriComponentsBuilder.fromUri(request.getUri())
					.scheme("https").build(true).toUri();
			return request.withNewUri(secureUri);
		}
		return request;
	}
}
