/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.ribbon.okhttp;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.springframework.cloud.netflix.ribbon.RibbonProperties;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToSecureConnectionIfNeeded;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 * @author Tim Ysewyn
 */
public class OkHttpLoadBalancingClient extends
		AbstractLoadBalancingClient<OkHttpRibbonRequest, OkHttpRibbonResponse, OkHttpClient> {

	public OkHttpLoadBalancingClient(IClientConfig config,
			ServerIntrospector serverIntrospector) {
		super(config, serverIntrospector);
	}

	public OkHttpLoadBalancingClient(OkHttpClient delegate, IClientConfig config,
			ServerIntrospector serverIntrospector) {
		super(delegate, config, serverIntrospector);
	}

	@Override
	protected OkHttpClient createDelegate(IClientConfig config) {
		return new OkHttpClient();
	}

	@Override
	public OkHttpRibbonResponse execute(OkHttpRibbonRequest ribbonRequest,
			final IClientConfig configOverride) throws Exception {
		boolean secure = isSecure(configOverride);
		if (secure) {
			final URI secureUri = UriComponentsBuilder.fromUri(ribbonRequest.getUri())
					.scheme("https").build().toUri();
			ribbonRequest = ribbonRequest.withNewUri(secureUri);
		}

		OkHttpClient httpClient = getOkHttpClient(configOverride, secure);
		final Request request = ribbonRequest.toRequest();
		Response response = httpClient.newCall(request).execute();
		return new OkHttpRibbonResponse(response, ribbonRequest.getUri());
	}

	OkHttpClient getOkHttpClient(IClientConfig configOverride, boolean secure) {
		IClientConfig config = configOverride != null ? configOverride : this.config;
		RibbonProperties ribbon = RibbonProperties.from(config);
		OkHttpClient.Builder builder = this.delegate.newBuilder()
				.connectTimeout(ribbon.connectTimeout(this.connectTimeout),
						TimeUnit.MILLISECONDS)
				.readTimeout(ribbon.readTimeout(this.readTimeout), TimeUnit.MILLISECONDS)
				.followRedirects(ribbon.isFollowRedirects(this.followRedirects));
		if (secure) {
			builder.followSslRedirects(ribbon.isFollowRedirects(this.followRedirects));
		}

		return builder.build();
	}

	@Override
	public URI reconstructURIWithServer(Server server, URI original) {
		URI uri = updateToSecureConnectionIfNeeded(original, this.config,
				this.serverIntrospector, server);
		return super.reconstructURIWithServer(server, uri);
	}

}
