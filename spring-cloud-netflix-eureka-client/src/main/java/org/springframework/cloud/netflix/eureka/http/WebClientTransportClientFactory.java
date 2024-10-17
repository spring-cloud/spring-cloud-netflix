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

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.extractUserInfo;
import static org.springframework.cloud.netflix.eureka.http.EurekaHttpClientUtils.objectMapper;

/**
 * Provides the custom {@link WebClient.Builder} required by the
 * {@link WebClientEurekaHttpClient}. Relies on Jackson for serialization and
 * deserialization.
 *
 * @author Daniel Lavoie
 * @author Haytham Mohamed
 * @author Armin Krezovic
 * @author Wonchul Heo
 */
public class WebClientTransportClientFactory implements TransportClientFactory {

	private final Supplier<WebClient.Builder> builderSupplier;

	public WebClientTransportClientFactory(Supplier<WebClient.Builder> builderSupplier) {
		this.builderSupplier = builderSupplier;
	}

	@Override
	public EurekaHttpClient newClient(EurekaEndpoint endpoint) {
		// we want a copy to modify. Don't change the original
		WebClient.Builder builder = this.builderSupplier.get().clone();
		setUrl(builder, endpoint.getServiceUrl());
		setCodecs(builder);
		builder.filter(http4XxErrorExchangeFilterFunction());
		return new WebClientEurekaHttpClient(builder.build());
	}

	private WebClient.Builder setUrl(WebClient.Builder builder, String serviceUrl) {
		String url = UriComponentsBuilder.fromUriString(serviceUrl).userInfo(null).toUriString();

		final EurekaHttpClientUtils.UserInfo userInfo = extractUserInfo(serviceUrl);
		if (userInfo != null) {
			builder.filter(ExchangeFilterFunctions.basicAuthentication(userInfo.username(), userInfo.password()));
		}
		return builder.baseUrl(url);
	}

	private void setCodecs(WebClient.Builder builder) {
		ObjectMapper objectMapper = objectMapper();
		builder.codecs(configurer -> {
			ClientCodecConfigurer.ClientDefaultCodecs defaults = configurer.defaultCodecs();
			defaults.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
			defaults.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));

		});
	}

	// Skip over 4xx http errors
	private ExchangeFilterFunction http4XxErrorExchangeFilterFunction() {
		return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
			// literally 400 pass the tests, not 4xxClientError
			if (clientResponse.statusCode().value() == 400) {
				ClientResponse newResponse = clientResponse.mutate().statusCode(HttpStatus.OK).build();
				newResponse.body((clientHttpResponse, context) -> clientHttpResponse.getBody());
				return Mono.just(newResponse);
			}
			if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
				ClientResponse newResponse = clientResponse.mutate()
					.statusCode(clientResponse.statusCode())
					// ignore body on 404 for heartbeat, see gh-4145
					.body(Flux.empty())
					.build();
				return Mono.just(newResponse);
			}
			return Mono.just(clientResponse);
		});
	}

	@Override
	public void shutdown() {
	}

}
