/*
 * Copyright 2017-2022 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
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
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.TransportClientFactory;

import org.springframework.cloud.configuration.SSLContextFactory;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Provides the custom {@link RestTemplate} required by the
 * {@link RestTemplateEurekaHttpClient}. Relies on Jackson for serialization and
 * deserialization.
 *
 * @author Daniel Lavoie
 */
public class RestTemplateTransportClientFactory implements TransportClientFactory {

	private final Optional<SSLContext> sslContext;

	private final Optional<HostnameVerifier> hostnameVerifier;

	private final EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier;

	public RestTemplateTransportClientFactory(TlsProperties tlsProperties,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier) {
		this.sslContext = context(tlsProperties);
		this.hostnameVerifier = Optional.empty();
		this.eurekaClientHttpRequestFactorySupplier = eurekaClientHttpRequestFactorySupplier;
	}

	private Optional<SSLContext> context(TlsProperties properties) {
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

	public RestTemplateTransportClientFactory(Optional<SSLContext> sslContext,
			Optional<HostnameVerifier> hostnameVerifier,
			EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier) {
		this.sslContext = sslContext;
		this.hostnameVerifier = hostnameVerifier;
		this.eurekaClientHttpRequestFactorySupplier = eurekaClientHttpRequestFactorySupplier;
	}

	public RestTemplateTransportClientFactory() {
		this.sslContext = Optional.empty();
		this.hostnameVerifier = Optional.empty();
		this.eurekaClientHttpRequestFactorySupplier = new DefaultEurekaClientHttpRequestFactorySupplier();
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
		RestTemplate restTemplate = new RestTemplate(requestFactory);

		try {
			URI serviceURI = new URI(serviceUrl);
			if (serviceURI.getUserInfo() != null) {
				String[] credentials = serviceURI.getUserInfo().split(":");
				if (credentials.length == 2) {
					restTemplate.getInterceptors()
							.add(new BasicAuthenticationInterceptor(credentials[0], credentials[1]));
				}
			}
		}
		catch (URISyntaxException ignore) {

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

	/**
	 * Provides the serialization configurations required by the Eureka Server. JSON
	 * content exchanged with eureka requires a root node matching the entity being
	 * serialized or deserialized. Achived with
	 * {@link SerializationFeature#WRAP_ROOT_VALUE} and
	 * {@link DeserializationFeature#UNWRAP_ROOT_VALUE}.
	 * {@link PropertyNamingStrategies.SnakeCaseStrategy} is applied to the underlying
	 * {@link ObjectMapper}.
	 * @return a {@link MappingJackson2HttpMessageConverter} object
	 */
	public MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setObjectMapper(new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));

		SimpleModule jsonModule = new SimpleModule();
		jsonModule.setSerializerModifier(createJsonSerializerModifier());
		converter.getObjectMapper().registerModule(jsonModule);

		converter.getObjectMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		converter.getObjectMapper().configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
		converter.getObjectMapper().addMixIn(Applications.class, ApplicationsJsonMixIn.class);
		converter.getObjectMapper().addMixIn(InstanceInfo.class, InstanceInfoJsonMixIn.class);

		return converter;
	}

	public static BeanSerializerModifier createJsonSerializerModifier() {
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

	@Override
	public void shutdown() {
	}

	/**
	 * Response that ignores body, specifically for 404 errors.
	 */
	private static class NotFoundHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse response;

		NotFoundHttpResponse(ClientHttpResponse response) {
			this.response = response;
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return response.getStatusCode();
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.getRawStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}

		@Override
		public void close() {
			response.close();
		}

		@Override
		public InputStream getBody() throws IOException {
			// ignore body on 404 for heartbeat, see gh-4145
			return null;
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}

	}

	class ErrorHandler extends DefaultResponseErrorHandler {

		@Override
		protected boolean hasError(HttpStatusCode statusCode) {
			/**
			 * When the Eureka server restarts and a client tries to sent a heartbeat the
			 * server will respond with a 404. By default RestTemplate will throw an
			 * exception in this case. What we want is to return the 404 to the upstream
			 * code so it will send another registration request to the server.
			 */
			if (statusCode.is4xxClientError()) {
				return false;
			}
			return super.hasError(statusCode);
		}

	}

}
