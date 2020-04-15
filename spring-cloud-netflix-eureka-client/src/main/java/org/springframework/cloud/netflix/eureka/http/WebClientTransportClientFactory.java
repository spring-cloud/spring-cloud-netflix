/*
 * Copyright 2017-2019 the original author or authors.
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
import java.util.Map;

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

import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

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
		return new WebClientEurekaHttpClient(webClient(serviceUrl.getServiceUrl()),
				serviceUrl.getServiceUrl());
	}

	private WebClient.Builder webClient(String serviceUrl) {
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

		WebClient.Builder builder = WebClient.builder().exchangeStrategies(strategies);

		// TODO: set a global error handler to skip over 4xx error type

		try {
			URI serviceURI = new URI(serviceUrl);
			if (serviceURI.getUserInfo() != null) {
				String[] credentials = serviceURI.getUserInfo().split(":");
				if (credentials.length == 2) {
					builder.filter(ExchangeFilterFunctions
							.basicAuthentication(credentials[0], credentials[1]));
				}
			}
		}
		catch (URISyntaxException ignore) {

		}

		return builder;
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

	public class ReactiveErrorAttributes extends DefaultErrorAttributes {

		@Override
		public Map<String, Object> getErrorAttributes(ServerRequest request,
				boolean includeStackTrace) {
			Map<String, Object> map = super.getErrorAttributes(request,
					includeStackTrace);
			map.put("status", request.exchange().getResponse().getStatusCode().value());
			return map;
		}

	}

	public class ReactiveErrorWebExceptionHandler
			extends AbstractErrorWebExceptionHandler {

		public ReactiveErrorWebExceptionHandler(ErrorAttributes errorAttributes,
				ResourceProperties resourceProperties,
				ApplicationContext applicationContext) {
			super(errorAttributes, resourceProperties, applicationContext);
		}

		@Override
		protected RouterFunction<ServerResponse> getRoutingFunction(
				ErrorAttributes errorAttributes) {

			return RouterFunctions.route(RequestPredicates.all(),
					this::renderErrorResponse);
		}

		private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {

			Map<String, Object> errors = getErrorAttributes(request, false);
			Integer httpStatusCode = (Integer) errors.get("status");

			if (String.valueOf(httpStatusCode).startsWith("4"))
				return Mono.empty();

			return ServerResponse.status(HttpStatus.BAD_REQUEST)
					.contentType(MediaType.APPLICATION_JSON_UTF8)
					.body(BodyInserters.fromObject(errors));
		}

	}

}
