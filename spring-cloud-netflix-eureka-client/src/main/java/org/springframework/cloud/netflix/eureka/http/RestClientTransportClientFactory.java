/*
 * Copyright 2017-2024 the original author or authors.
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

import java.util.Optional;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.TransportClientFactory;

import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.context;
import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.extractUserInfo;
import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.mappingJacksonHttpMessageConverter;

/**
 * Provides the custom {@link RestClient} required by the
 * {@link RestClientEurekaHttpClient}. Relies on Jackson for serialization and
 * deserialization.
 *
 * @author Wonchul Heo
 * @author Olga Maciaszek-Sharma
 * @since 4.2.0
 */
public class RestClientTransportClientFactory implements TransportClientFactory {

	private final Optional<SSLContext> sslContext;

	private final Optional<HostnameVerifier> hostnameVerifier;

	private final EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier;

	private final Supplier<RestClient.Builder> builderSupplier;

	public RestClientTransportClientFactory(Optional<SSLContext> sslContext,
			Optional<HostnameVerifier> hostnameVerifier,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
			Supplier<RestClient.Builder> builderSupplier) {
		this.sslContext = sslContext;
		this.hostnameVerifier = hostnameVerifier;
		this.eurekaClientHttpRequestFactorySupplier = eurekaClientHttpRequestFactorySupplier;
		this.builderSupplier = builderSupplier;
	}

	public RestClientTransportClientFactory(TlsProperties tlsProperties,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
			Supplier<RestClient.Builder> builderSupplier) {
		this(context(tlsProperties), Optional.empty(), eurekaClientHttpRequestFactorySupplier, builderSupplier);
	}

	public RestClientTransportClientFactory(TlsProperties tlsProperties,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier) {
		this(tlsProperties, eurekaClientHttpRequestFactorySupplier, RestClient::builder);
	}

	@Override
	public EurekaHttpClient newClient(EurekaEndpoint endpoint) {
		// we want a copy to modify. Don't change the original
		final RestClient.Builder builder = builderSupplier.get().clone();

		ClientHttpRequestFactory requestFactory = this.eurekaClientHttpRequestFactorySupplier
			.get(this.sslContext.orElse(null), this.hostnameVerifier.orElse(null));

		builder.requestFactory(requestFactory);
		setUrl(builder, endpoint.getServiceUrl());
		builder.messageConverters(converters -> converters.add(0, mappingJacksonHttpMessageConverter()));

		builder.defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
		});

		builder.requestInterceptor((request, body, execution) -> {
			final ClientHttpResponse response = execution.execute(request, body);
			if (!response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				return response;
			}
			return new NotFoundHttpResponse(response);
		});

		return new RestClientEurekaHttpClient(builder.build());
	}

	@Override
	public void shutdown() {
	}

	private static void setUrl(RestClient.Builder builder, String serviceUrl) {
		final String url = UriComponentsBuilder.fromUriString(serviceUrl).userInfo(null).toUriString();

		final EurekaHttpClientUtils.UserInfo userInfo = extractUserInfo(serviceUrl);
		if (userInfo != null) {
			builder.requestInterceptor(new BasicAuthenticationInterceptor(userInfo.username(), userInfo.password()));
		}
		builder.baseUrl(url);
	}

}
