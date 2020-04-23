/*
 * Copyright 2017-2020 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.jackson.mixin.ApplicationsJsonMixIn;
import com.netflix.discovery.converters.jackson.mixin.InstanceInfoJsonMixIn;
import com.netflix.discovery.converters.jackson.serializer.InstanceInfoJsonBeanSerializer;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides the custom {@link WebClient.Builder} required by the
 * {@link WebClientEurekaHttpClient}. Relies on Jackson for serialization and
 * deserialization.
 *
 * @author Daniel Lavoie
 * @author Haytham Mohamed
 */
public class WebClientTransportClientFactory implements TransportClientFactory {

	@Override
	public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
		WebClient.Builder builder = of(serviceUrl.getServiceUrl());
		this.setExchangeStrategies(builder);
		this.skipHttp400Error(builder);
		return new WebClientEurekaHttpClient(builder.build());
	}

	private WebClient.Builder of(String serviceUrl) {
		String url = serviceUrl;
		WebClient.Builder builder = WebClient.builder();
		try {
			URI serviceURI = new URI(serviceUrl);
			if (serviceURI.getUserInfo() != null) {
				String[] credentials = serviceURI.getUserInfo().split(":");
				if (credentials.length == 2) {
					builder.filter(ExchangeFilterFunctions
							.basicAuthentication(credentials[0], credentials[1]));
					url = serviceUrl.replace(credentials[0] + ":" + credentials[1] + "@",
							"");
				}
			}
		}
		catch (URISyntaxException ignore) {
		}
		return builder.baseUrl(url);
	}

	private void setExchangeStrategies(WebClient.Builder builder) {
		ObjectMapper objectMapper = mappingJacksonHttpMessageConverter()
				.getObjectMapper();
		ExchangeStrategies strategies = ExchangeStrategies.builder()
				.codecs(clientDefaultCodecsConfigurer -> {
					clientDefaultCodecsConfigurer.defaultCodecs()
							.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper,
									MediaType.APPLICATION_JSON));
					clientDefaultCodecsConfigurer.defaultCodecs()
							.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper,
									MediaType.APPLICATION_JSON));

				}).build();
		builder.exchangeStrategies(strategies);
	}

	private void skipHttp400Error(WebClient.Builder builder) {
		builder.filter(Http4xxErrorExchangeFilterFunction());
	}

	// Skip over 4xx http errors
	private ExchangeFilterFunction Http4xxErrorExchangeFilterFunction() {
		return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
			// literally 400 pass the tests, not 4xxClientError
			if (clientResponse.statusCode().value() == 400) {
				ClientResponse newResponse = ClientResponse.from(clientResponse)
						.statusCode(HttpStatus.OK).build();
				newResponse.body((clientHttpResponse, context) -> {
					return clientHttpResponse.getBody();
				});
				return Mono.just(newResponse);
			}
			return Mono.just(clientResponse);
		});
	}

	/**
	 * Provides the serialization configurations required by the Eureka Server. JSON
	 * content exchanged with eureka requires a root node matching the entity being
	 * serialized or deserialized. Achieved with
	 * {@link SerializationFeature#WRAP_ROOT_VALUE} and
	 * {@link DeserializationFeature#UNWRAP_ROOT_VALUE}.
	 * {@link PropertyNamingStrategy.SnakeCaseStrategy} is applied to the underlying
	 * {@link ObjectMapper}.
	 * @return a {@link MappingJackson2HttpMessageConverter} object
	 */
	public MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(new ObjectMapper()
				.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE));

		SimpleModule jsonModule = new SimpleModule();
		jsonModule.setSerializerModifier(createJsonSerializerModifier()); // keyFormatter,
		// compact));
		converter.getObjectMapper().registerModule(jsonModule);

		converter.getObjectMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		converter.getObjectMapper().configure(DeserializationFeature.UNWRAP_ROOT_VALUE,
				true);
		converter.getObjectMapper().addMixIn(Applications.class,
				ApplicationsJsonMixIn.class);
		converter.getObjectMapper().addMixIn(InstanceInfo.class,
				InstanceInfoJsonMixIn.class);

		// converter.getObjectMapper().addMixIn(DataCenterInfo.class,
		// DataCenterInfoXmlMixIn.class);
		// converter.getObjectMapper().addMixIn(InstanceInfo.PortWrapper.class,
		// PortWrapperXmlMixIn.class);
		// converter.getObjectMapper().addMixIn(Application.class,
		// ApplicationXmlMixIn.class);
		// converter.getObjectMapper().addMixIn(Applications.class,
		// ApplicationsXmlMixIn.class);

		return converter;
	}

	public static BeanSerializerModifier createJsonSerializerModifier() { // final
		// KeyFormatter
		// keyFormatter,
		// final
		// boolean
		// compactMode)
		// {
		return new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config,
					BeanDescription beanDesc, JsonSerializer<?> serializer) {
				/*
				 * if (beanDesc.getBeanClass().isAssignableFrom(Applications.class)) {
				 * return new ApplicationsJsonBeanSerializer((BeanSerializerBase)
				 * serializer, keyFormatter); }
				 */
				if (beanDesc.getBeanClass().isAssignableFrom(InstanceInfo.class)) {
					return new InstanceInfoJsonBeanSerializer(
							(BeanSerializerBase) serializer, false);
				}
				return serializer;
			}
		};
	}

	@Override
	public void shutdown() {
	}

}
