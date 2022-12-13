/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.cloud.netflix.eureka.RestTemplateTimeoutProperties;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;

/**
 * Supplier for the {@link ClientHttpRequestFactory} to be used by Eureka client that uses
 * {@link HttpClients}.
 *
 * @author Marcin Grzejszczak
 * @author Jiwon Jeon
 * @since 3.0.0
 */
public class DefaultEurekaClientHttpRequestFactorySupplier implements EurekaClientHttpRequestFactorySupplier {

	private final RestTemplateTimeoutProperties restTemplateTimeoutProperties;

	/**
	 * @deprecated in favour of
	 * {@link DefaultEurekaClientHttpRequestFactorySupplier#DefaultEurekaClientHttpRequestFactorySupplier(RestTemplateTimeoutProperties)}
	 */
	@Deprecated
	public DefaultEurekaClientHttpRequestFactorySupplier() {
		this.restTemplateTimeoutProperties = new RestTemplateTimeoutProperties();
	}

	public DefaultEurekaClientHttpRequestFactorySupplier(RestTemplateTimeoutProperties restTemplateTimeoutProperties) {
		this.restTemplateTimeoutProperties = restTemplateTimeoutProperties;
	}

	@Override
	public ClientHttpRequestFactory get(SSLContext sslContext, @Nullable HostnameVerifier hostnameVerifier) {
		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		if (sslContext != null) {
			httpClientBuilder = httpClientBuilder.setSSLContext(sslContext);
		}
		if (hostnameVerifier != null) {
			httpClientBuilder = httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
		}
		if (restTemplateTimeoutProperties != null) {
			httpClientBuilder.setDefaultRequestConfig(buildRequestConfig());
		}

		CloseableHttpClient httpClient = httpClientBuilder.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		return requestFactory;
	}

	private RequestConfig buildRequestConfig() {
		return RequestConfig.custom().setConnectTimeout(restTemplateTimeoutProperties.getConnectTimeout())
				.setConnectionRequestTimeout(restTemplateTimeoutProperties.getConnectRequestTimeout())
				.setSocketTimeout(restTemplateTimeoutProperties.getSocketTimeout()).build();
	}

}
