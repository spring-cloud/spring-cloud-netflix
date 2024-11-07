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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
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
import com.netflix.discovery.shared.transport.EurekaHttpClient;

import org.springframework.cloud.configuration.SSLContextFactory;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;

/**
 * Utility class for dealing with {@link EurekaHttpClient}.
 *
 * @author Daniel Lavoie
 * @author Wonchul Heo
 * @author Olga Maciaszek-Sharma
 * @since 4.2.0
 */
final class EurekaHttpClientUtils {

	private EurekaHttpClientUtils() {
		throw new AssertionError("Must not instantiate constant utility class");
	}

	/**
	 * Provides the serialization configurations required by the Eureka Server. JSON
	 * content exchanged with eureka requires a root node matching the entity being
	 * serialized or deserialized. Achieved with
	 * {@link SerializationFeature#WRAP_ROOT_VALUE} and
	 * {@link DeserializationFeature#UNWRAP_ROOT_VALUE}.
	 * {@link PropertyNamingStrategies.SnakeCaseStrategy} is applied to the underlying
	 * {@link ObjectMapper}.
	 * @return a {@link MappingJackson2HttpMessageConverter} object
	 */
	static MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(objectMapper());
		return converter;
	}

	/**
	 * Provides the serialization configurations required by the Eureka Server. JSON
	 * content exchanged with eureka requires a root node matching the entity being
	 * serialized or deserialized. Achieved with
	 * {@link SerializationFeature#WRAP_ROOT_VALUE} and
	 * {@link DeserializationFeature#UNWRAP_ROOT_VALUE}.
	 * {@link PropertyNamingStrategies.SnakeCaseStrategy} is applied to the underlying
	 * {@link ObjectMapper}.
	 * @return a {@link ObjectMapper} object
	 */
	static ObjectMapper objectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		final SimpleModule jsonModule = new SimpleModule();
		jsonModule.setSerializerModifier(createJsonSerializerModifier());
		objectMapper.registerModule(jsonModule);

		objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
		objectMapper.addMixIn(Applications.class, ApplicationsJsonMixIn.class);
		objectMapper.addMixIn(InstanceInfo.class, InstanceInfoJsonMixIn.class);

		return objectMapper;
	}

	private static BeanSerializerModifier createJsonSerializerModifier() {
		return new BeanSerializerModifier() {
			@Override
			public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
					JsonSerializer<?> serializer) {
				if (beanDesc.getBeanClass().isAssignableFrom(InstanceInfo.class)) {
					return new InstanceInfoJsonBeanSerializer((BeanSerializerBase) serializer, false);
				}
				return serializer;
			}
		};
	}

	@Nullable
	static UserInfo extractUserInfo(String serviceUrl) {
		try {
			final URI serviceURI = new URI(serviceUrl);
			if (serviceURI.getUserInfo() != null) {
				final String[] credentials = serviceURI.getUserInfo().split(":");
				if (credentials.length == 2) {
					return new UserInfo(credentials[0], credentials[1]);
				}
			}
		}
		catch (URISyntaxException ignore) {
		}
		return null;
	}

	static Optional<SSLContext> context(TlsProperties properties) {
		if (properties == null || !properties.isEnabled()) {
			return Optional.empty();
		}
		try {
			return Optional.of(new SSLContextFactory(properties).createSSLContext());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	record UserInfo(String username, String password) {
	}

}
