/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

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

/**
 * Provides the custom {@link RestTemplate} required by the
 * {@link RestTemplateEurekaHttpClient}. Relies on Jackson for serialization and
 * deserialization.
 * 
 * @author Daniel Lavoie
 */
public class RestTemplateTransportClientFactory implements TransportClientFactory {

	@Override
	public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
		return new RestTemplateEurekaHttpClient(restTemplate(serviceUrl.getServiceUrl()),
				serviceUrl.getServiceUrl());
	}

	private RestTemplate restTemplate(String serviceUrl) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI serviceURI = new URI(serviceUrl);
			if (serviceURI.getUserInfo() != null) {
				String[] credentials = serviceURI.getUserInfo().split(":");
				if (credentials.length == 2) {
					restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(
							credentials[0], credentials[1]));
				}
			}
		}
		catch (URISyntaxException ignore) {

		}

		restTemplate.getMessageConverters().add(0, mappingJacksonHttpMessageConverter());

		return restTemplate;
	}

	/**
	 * Provides the serialization configurations required by the Eureka Server. JSON
	 * content exchanged with eureka requires a root node matching the entity being
	 * serialized or deserialized. Achived with
	 * {@link SerializationFeature.WRAP_ROOT_VALUE} and
	 * {@link DeserializationFeature.UNWRAP_ROOT_VALUE}.
	 * {@link PropertyNamingStrategy.SnakeCaseStrategy} is applied to the underlying
	 * {@link ObjectMapper}.
	 * 
	 * 
	 * @return
	 */
	public MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(new ObjectMapper()
				.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE));

		SimpleModule jsonModule = new SimpleModule();
		jsonModule.setSerializerModifier(createJsonSerializerModifier());//keyFormatter, compact));
		converter.getObjectMapper().registerModule(jsonModule);

		converter.getObjectMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		converter.getObjectMapper().configure(DeserializationFeature.UNWRAP_ROOT_VALUE,
				true);
		converter.getObjectMapper().addMixIn(Applications.class, ApplicationsJsonMixIn.class);
		converter.getObjectMapper().addMixIn(InstanceInfo.class, InstanceInfoJsonMixIn.class);

		// converter.getObjectMapper().addMixIn(DataCenterInfo.class, DataCenterInfoXmlMixIn.class);
		// converter.getObjectMapper().addMixIn(InstanceInfo.PortWrapper.class, PortWrapperXmlMixIn.class);
		// converter.getObjectMapper().addMixIn(Application.class, ApplicationXmlMixIn.class);
		// converter.getObjectMapper().addMixIn(Applications.class, ApplicationsXmlMixIn.class);


		return converter;
	}

	public static BeanSerializerModifier createJsonSerializerModifier() {//final KeyFormatter keyFormatter, final boolean compactMode) {
		return new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config,
													  BeanDescription beanDesc, JsonSerializer<?> serializer) {
				/*if (beanDesc.getBeanClass().isAssignableFrom(Applications.class)) {
					return new ApplicationsJsonBeanSerializer((BeanSerializerBase) serializer, keyFormatter);
				}*/
				if (beanDesc.getBeanClass().isAssignableFrom(InstanceInfo.class)) {
					return new InstanceInfoJsonBeanSerializer((BeanSerializerBase) serializer, false);
				}
				return serializer;
			}
		};
	}

	@Override
	public void shutdown() {
	}

}
