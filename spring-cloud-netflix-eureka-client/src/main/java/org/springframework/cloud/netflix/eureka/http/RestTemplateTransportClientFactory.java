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

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.context;
import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.extractUserInfo;
import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.mappingJacksonHttpMessageConverter;

/**
 * Provides the custom {@link RestTemplate} required by the
 * {@link RestTemplateEurekaHttpClient}. Relies on Jackson for serialization and
 * deserialization.
 *
 * @author Daniel Lavoie
 * @author Armin Krezovic
 * @author Wonchul Heo
 * @author Olga Maciaszek-Sharma
 * @deprecated {@link RestTemplate}-based implementation to be removed in favour of
 * {@link RestClient}-based implementation.
 */
@Deprecated(forRemoval = true)
public class RestTemplateTransportClientFactory implements TransportClientFactory {

	private final Optional<SSLContext> sslContext;

	private final Optional<HostnameVerifier> hostnameVerifier;

	private final EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier;

	private final Supplier<RestTemplateBuilder> restTemplateBuilderSupplier;

	public RestTemplateTransportClientFactory(Optional<SSLContext> sslContext,
			Optional<HostnameVerifier> hostnameVerifier,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
			Supplier<RestTemplateBuilder> restTemplateBuilderSupplier) {
		this.sslContext = sslContext;
		this.hostnameVerifier = hostnameVerifier;
		this.eurekaClientHttpRequestFactorySupplier = eurekaClientHttpRequestFactorySupplier;
		this.restTemplateBuilderSupplier = restTemplateBuilderSupplier;
	}

	public RestTemplateTransportClientFactory(TlsProperties tlsProperties,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier,
			Supplier<RestTemplateBuilder> restTemplateBuilderSupplier) {
		this(context(tlsProperties), Optional.empty(), eurekaClientHttpRequestFactorySupplier,
				restTemplateBuilderSupplier);
	}

	public RestTemplateTransportClientFactory(TlsProperties tlsProperties,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier) {
		this(tlsProperties, eurekaClientHttpRequestFactorySupplier, RestTemplateBuilder::new);
	}

	public RestTemplateTransportClientFactory(Optional<SSLContext> sslContext,
			Optional<HostnameVerifier> hostnameVerifier,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier) {

		this(sslContext, hostnameVerifier, eurekaClientHttpRequestFactorySupplier, RestTemplateBuilder::new);
	}

	// Visible for testing
	/**
	 * @deprecated pass the default values while initialising object in test classes.
	 */
	@Deprecated(forRemoval = true)
	public RestTemplateTransportClientFactory() {
		this(Optional.empty(), Optional.empty(), new DefaultEurekaClientHttpRequestFactorySupplier());
	}

	@Override
	public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
		return new RestTemplateEurekaHttpClient(restTemplate(serviceUrl.getServiceUrl()),
				stripUserInfo(serviceUrl.getServiceUrl()));
	}

	// apache http client 5.2 fails with non-null userinfo
	// basic auth added in restTemplate() below
	private String stripUserInfo(String serviceUrl) {
		return UriComponentsBuilder.fromUriString(serviceUrl).userInfo(null).toUriString();
	}

	private RestTemplate restTemplate(String serviceUrl) {
		ClientHttpRequestFactory requestFactory = this.eurekaClientHttpRequestFactorySupplier
			.get(this.sslContext.orElse(null), this.hostnameVerifier.orElse(null));

		RestTemplate restTemplate;

		if (restTemplateBuilderSupplier != null && restTemplateBuilderSupplier.get() != null) {
			restTemplate = restTemplateBuilderSupplier.get().requestFactory(() -> requestFactory).build();
		}
		else {
			restTemplate = new RestTemplate(requestFactory);
		}

		final EurekaHttpClientUtils.UserInfo userInfo = extractUserInfo(serviceUrl);
		if (userInfo != null) {
			restTemplate.getInterceptors()
				.add(new BasicAuthenticationInterceptor(userInfo.username(), userInfo.password()));
		}

		restTemplate.getMessageConverters().add(0, mappingJacksonHttpMessageConverter());
		restTemplate.setErrorHandler(new ErrorHandler());

		restTemplate.getInterceptors().add((request, body, execution) -> {
			ClientHttpResponse response = execution.execute(request, body);
			if (!response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				return response;
			}
			return new NotFoundHttpResponse(response);
		});

		return restTemplate;
	}

	@Override
	public void shutdown() {
	}

	class ErrorHandler extends DefaultResponseErrorHandler {

		@Override
		protected boolean hasError(HttpStatusCode statusCode) {
			/**
			 * When the Eureka server restarts and a client tries to send a heartbeat the
			 * server will respond with a 404. By default, RestTemplate will throw an
			 * exception in this case. What we want is to return the 404 to the upstream
			 * code, so it will send another registration request to the server.
			 */
			if (statusCode.is4xxClientError()) {
				return false;
			}
			return super.hasError(statusCode);
		}

	}

}
