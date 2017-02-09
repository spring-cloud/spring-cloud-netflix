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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.support.RetryableLoadBalancingClient;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
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
		extends RetryableLoadBalancingClient<OkHttpRibbonRequest, OkHttpRibbonResponse, OkHttpClient> {

	@Deprecated
	public OkHttpLoadBalancingClient() {
		super();
	}

	@Deprecated
	public OkHttpLoadBalancingClient(final ILoadBalancer lb) {
		super(lb);
	}

	public OkHttpLoadBalancingClient(IClientConfig config,
			ServerIntrospector serverIntrospector) {
		super(config, serverIntrospector);
	}

	public OkHttpLoadBalancingClient(IClientConfig config,
									 ServerIntrospector serverIntrospector,
									 LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory) {
		super(config, serverIntrospector, loadBalancedRetryPolicyFactory);
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
	public OkHttpRibbonResponse execute(final OkHttpRibbonRequest ribbonRequest,
			final IClientConfig configOverride) throws Exception {
		return this.executeWithRetry(ribbonRequest, new RetryCallback() {
			@Override
			public OkHttpRibbonResponse doWithRetry(RetryContext context) throws Exception {
				//on retries the policy will choose the server and set it in the context
				//extract the server and update the request being made
				OkHttpRibbonRequest newRequest = ribbonRequest;
				if(context instanceof LoadBalancedRetryContext) {
					ServiceInstance service = ((LoadBalancedRetryContext)context).getServiceInstance();
					if(service != null) {
						//Reconstruct the request URI using the host and port set in the retry context
						newRequest = newRequest.withNewUri(new URI(service.getUri().getScheme(),
								newRequest.getURI().getUserInfo(), service.getHost(), service.getPort(),
								newRequest.getURI().getPath(), newRequest.getURI().getQuery(),
								newRequest.getURI().getFragment()));
					}
				}
				if (isSecure(configOverride)) {
					final URI secureUri = UriComponentsBuilder.fromUri(newRequest.getUri())
							.scheme("https").build().toUri();
					newRequest = newRequest.withNewUri(secureUri);
				}
				OkHttpClient httpClient = getOkHttpClient(configOverride, secure);

				final Request request = newRequest.toRequest();
				Response response = httpClient.newCall(request).execute();
				return new OkHttpRibbonResponse(response, newRequest.getUri());
			}
		});
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
