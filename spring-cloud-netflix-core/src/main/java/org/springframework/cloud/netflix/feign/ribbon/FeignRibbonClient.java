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

package org.springframework.cloud.netflix.feign.ribbon;

import java.io.IOException;
import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.util.ReflectionUtils;

import com.netflix.client.ClientException;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

import dagger.Lazy;
import feign.Client;
import feign.Request;
import feign.Response;

/**
 * @author Julien Roy
 * @author Spencer Gibb
 */
public class FeignRibbonClient implements Client {

	private Client defaultClient = new Default(new LazySSLSocketFactory(),
			new LazyHostnameVerifier());

	private SpringClientFactory factory;

	public FeignRibbonClient(SpringClientFactory factory) {
		this.factory = factory;
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		try {
			URI asUri = URI.create(request.url());
			String clientName = asUri.getHost();
			URI uriWithoutSchemeAndPort = URI.create(request.url().replace(
					asUri.getScheme() + "://" + asUri.getHost(), ""));
			RibbonLoadBalancer.RibbonRequest ribbonRequest = new RibbonLoadBalancer.RibbonRequest(
					request, uriWithoutSchemeAndPort);
			return lbClient(clientName).executeWithLoadBalancer(ribbonRequest)
					.toResponse();
		}
		catch (ClientException ex) {
			if (ex.getCause() instanceof IOException) {
				throw IOException.class.cast(ex.getCause());
			}
			ReflectionUtils.rethrowRuntimeException(ex);
			return null;
		}
	}

	private RibbonLoadBalancer lbClient(String clientName) {
		IClientConfig config = this.factory.getClientConfig(clientName);
		ILoadBalancer lb = this.factory.getLoadBalancer(clientName);
		return new RibbonLoadBalancer(this.defaultClient, lb, config);
	}

	public void setDefaultClient(Client defaultClient) {
		this.defaultClient = defaultClient;
	}

	private static class LazySSLSocketFactory implements Lazy<SSLSocketFactory> {

		@Override
		public SSLSocketFactory get() {
			return (SSLSocketFactory) SSLSocketFactory.getDefault();
		}

	}

	private static class LazyHostnameVerifier implements Lazy<HostnameVerifier> {

		@Override
		public HostnameVerifier get() {
			return HttpsURLConnection.getDefaultHostnameVerifier();
		}

	}

}
