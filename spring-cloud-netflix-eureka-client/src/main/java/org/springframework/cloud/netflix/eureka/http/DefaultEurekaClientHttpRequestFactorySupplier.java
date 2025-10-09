/*
 * Copyright 2013-present the original author or authors.
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

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;

import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;

/**
 * Supplier for the {@link ClientHttpRequestFactory} to be used by Eureka client that uses
 * {@link HttpClients}.
 *
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 * @author Jiwon Jeon
 * @since 3.0.0
 */
public class DefaultEurekaClientHttpRequestFactorySupplier implements EurekaClientHttpRequestFactorySupplier {

	private final TimeoutProperties timeoutProperties;

	private final Set<RequestConfigCustomizer> requestConfigCustomizers;

	public DefaultEurekaClientHttpRequestFactorySupplier(TimeoutProperties timeoutProperties,
			Set<RequestConfigCustomizer> requestConfigCustomizers) {
		this.timeoutProperties = timeoutProperties;
		this.requestConfigCustomizers = requestConfigCustomizers;
	}

	@Override
	public ClientHttpRequestFactory get(SSLContext sslContext, @Nullable HostnameVerifier hostnameVerifier) {
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		if (sslContext != null || hostnameVerifier != null || timeoutProperties != null) {
			httpClientBuilder
				.setConnectionManager(buildConnectionManager(sslContext, hostnameVerifier, timeoutProperties));
		}
		httpClientBuilder.setDefaultRequestConfig(buildRequestConfig());

		CloseableHttpClient httpClient = httpClientBuilder.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		return requestFactory;
	}

	private HttpClientConnectionManager buildConnectionManager(SSLContext sslContext, HostnameVerifier hostnameVerifier,
			TimeoutProperties timeoutProperties) {
		PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
			.create();
		SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder = SSLConnectionSocketFactoryBuilder
			.create();
		if (sslContext != null) {
			sslConnectionSocketFactoryBuilder.setSslContext(sslContext);
		}
		if (hostnameVerifier != null) {
			sslConnectionSocketFactoryBuilder.setHostnameVerifier(hostnameVerifier);
		}
		connectionManagerBuilder.setSSLSocketFactory(sslConnectionSocketFactoryBuilder.build());
		if (timeoutProperties != null) {
			connectionManagerBuilder.setDefaultSocketConfig(SocketConfig.custom()
				.setSoTimeout(Timeout.of(timeoutProperties.getSocketTimeout(), TimeUnit.MILLISECONDS))
				.build());
			connectionManagerBuilder.setDefaultConnectionConfig(ConnectionConfig.custom()
				.setConnectTimeout(Timeout.of(timeoutProperties.getConnectTimeout(), TimeUnit.MILLISECONDS))
				.build());
		}
		return connectionManagerBuilder.build();
	}

	private RequestConfig buildRequestConfig() {
		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
		if (timeoutProperties != null) {
			requestConfigBuilder.setConnectionRequestTimeout(
					Timeout.of(timeoutProperties.getConnectRequestTimeout(), TimeUnit.MILLISECONDS));
		}
		requestConfigCustomizers.forEach(customizer -> customizer.customize(requestConfigBuilder));
		return requestConfigBuilder.build();
	}

}
