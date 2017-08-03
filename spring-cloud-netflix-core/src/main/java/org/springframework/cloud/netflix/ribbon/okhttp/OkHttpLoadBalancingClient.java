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

package org.springframework.cloud.netflix.ribbon.okhttp;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToHttpsIfNeeded;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
public class OkHttpLoadBalancingClient
		extends AbstractLoadBalancingClient<OkHttpRibbonRequest, OkHttpRibbonResponse, OkHttpClient> {

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
		OkHttpClient.Builder builder = this.delegate.newBuilder();
		IClientConfig config = configOverride != null ? configOverride : this.config;
		builder.connectTimeout(config.get(
				CommonClientConfigKey.ConnectTimeout, this.connectTimeout), TimeUnit.MILLISECONDS);
		builder.readTimeout(config.get(
				CommonClientConfigKey.ReadTimeout, this.readTimeout), TimeUnit.MILLISECONDS);
		builder.followRedirects(config.get(
				CommonClientConfigKey.FollowRedirects, this.followRedirects));
		if (secure) {
			builder.followSslRedirects(configOverride.get(
					CommonClientConfigKey.FollowRedirects, this.followRedirects));
		}

		return builder.build();
	}

	@Override
	public URI reconstructURIWithServer(Server server, URI original) {
		URI uri = updateToHttpsIfNeeded(original, this.config, this.serverIntrospector,
				server);
		return super.reconstructURIWithServer(server, uri);
	}
}
