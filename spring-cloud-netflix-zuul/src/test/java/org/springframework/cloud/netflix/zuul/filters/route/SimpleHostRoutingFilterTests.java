/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.monitoring.CounterFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.metrics.EmptyCounterFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.StreamUtils.copyToByteArray;

/**
 * @author Andreas Kluth
 * @author Spencer Gibb
 * @author Gang Li
 * @author Denys Ivano
 */
public class SimpleHostRoutingFilterTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void setup() {
		CounterFactory.initialize(new EmptyCounterFactory());
		RequestContext.testSetCurrentContext(new RequestContext());
	}

	@After
	public void clear() {
		if (this.context != null) {
			this.context.close();
		}
		CounterFactory.initialize(null);

		RequestContext.testSetCurrentContext(null);
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void timeoutPropertiesAreApplied() {
		TestPropertyValues
				.of("zuul.host.socket-timeout-millis=11000",
						"zuul.host.connect-timeout-millis=2100",
						"zuul.host.connection-request-timeout-millis=2500")
				.applyTo(this.context);
		setupContext();
		CloseableHttpClient httpClient = getFilter().newClient();
		Assertions.assertThat(httpClient).isInstanceOf(Configurable.class);
		RequestConfig config = ((Configurable) httpClient).getConfig();
		assertThat(config.getSocketTimeout()).isEqualTo(11000);
		assertThat(config.getConnectTimeout()).isEqualTo(2100);
		assertThat(config.getConnectionRequestTimeout()).isEqualTo(2500);
	}

	@Test
	public void connectionPropertiesAreApplied() {
		TestPropertyValues.of("zuul.host.maxTotalConnections=100",
				"zuul.host.maxPerRouteConnections=10", "zuul.host.timeToLive=5",
				"zuul.host.timeUnit=SECONDS").applyTo(this.context);
		setupContext();
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager) getFilter()
				.getConnectionManager();
		assertThat(connMgr.getMaxTotal()).isEqualTo(100);
		assertThat(connMgr.getDefaultMaxPerRoute()).isEqualTo(10);
		Object pool = getField(connMgr, "pool");
		assertThat(pool).hasFieldOrPropertyWithValue("timeToLive", 5L);
		assertThat(pool).hasFieldOrPropertyWithValue("timeUnit", TimeUnit.SECONDS);
	}

	@SuppressWarnings("unchecked")
	private <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			throw new IllegalArgumentException(
					target.getClass() + " does not have field " + name);
		}
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

	@Test
	public void validateSslHostnamesByDefault() {
		setupContext();
		assertThat(getFilter().isSslHostnameValidationEnabled())
				.as("Hostname verification should be enabled by default").isTrue();
	}

	@Test
	public void validationOfSslHostnamesCanBeDisabledViaProperty() {
		TestPropertyValues.of("zuul.sslHostnameValidationEnabled=false")
				.applyTo(this.context);
		setupContext();
		assertThat(getFilter().isSslHostnameValidationEnabled())
				.as("Hostname verification should be disabled via property").isFalse();
	}

	@Test
	public void defaultPropertiesAreApplied() {
		setupContext();
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager) getFilter()
				.getConnectionManager();

		assertThat(connMgr.getMaxTotal()).isEqualTo(200);
		assertThat(connMgr.getDefaultMaxPerRoute()).isEqualTo(20);
	}

	@Test
	public void deleteRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(
				new ByteArrayInputStream(new byte[] { 1 }));
		HttpRequest httpRequest = getFilter().buildHttpRequest("DELETE", "uri",
				inputStreamEntity, new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertThat(httpRequest instanceof HttpEntityEnclosingRequest).isTrue();
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertThat(httpEntityEnclosingRequest.getEntity() != null).isTrue();
	}

	@Test
	public void zuulHostKeysUpdateHttpClient() {
		setupContext();
		SimpleHostRoutingFilter filter = getFilter();
		CloseableHttpClient httpClient = extractHttpClient(filter);
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(
				Collections.singleton("zuul.host.mykey"));
		filter.onApplicationEvent(event);
		CloseableHttpClient newhttpClient = extractHttpClient(filter);
		Assertions.assertThat(httpClient).isNotEqualTo(newhttpClient);
	}

	private CloseableHttpClient extractHttpClient(SimpleHostRoutingFilter filter) {
		return (CloseableHttpClient) ReflectionTestUtils.getField(filter, "httpClient");
	}

	@Test
	public void zuulHostKeysUpdateHttpClientConnectionManagerIsNotShutDown() {
		setupContext();
		SimpleHostRoutingFilter filter = getFilter();
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(
				Collections.singleton("zuul.host.mykey"));
		filter.onApplicationEvent(event);

		CloseableHttpClient httpClient = extractHttpClient(filter);
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager) extractConnectionManager(
				httpClient);
		AtomicBoolean isShutDown = getField(connMgr, "isShutDown");
		assertThat(isShutDown.get()).as("Connection manager shut down").isFalse();
		Object pool = getField(connMgr, "pool");
		assertThat(pool).hasFieldOrPropertyWithValue("isShutDown", false);
	}

	private HttpClientConnectionManager extractConnectionManager(
			CloseableHttpClient httpClient) {
		// Default implementation is org.apache.http.impl.client.InternalHttpClient
		return getField(httpClient, "connManager");
	}

	@Test
	public void zuulHostKeysUpdateHttpClientUsesNewConnectionManager() {
		setupContext();
		SimpleHostRoutingFilter filter = getFilter();
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager) filter
				.getConnectionManager();
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(
				Collections.singleton("zuul.host.mykey"));
		filter.onApplicationEvent(event);

		PoolingHttpClientConnectionManager newConnMgr = (PoolingHttpClientConnectionManager) filter
				.getConnectionManager();
		CloseableHttpClient httpClient = extractHttpClient(filter);
		HttpClientConnectionManager usedConnMgr = extractConnectionManager(httpClient);
		assertThat(usedConnMgr).isNotEqualTo(connMgr);
		assertThat(usedConnMgr).isEqualTo(newConnMgr);
	}

	@Test
	public void zuulHostKeysUpdateConnectionManagerPropertiesAreChanged() {
		setupContext();
		SimpleHostRoutingFilter filter = getFilter();
		ZuulProperties.Host host = context.getBean(ZuulProperties.class).getHost();
		host.setMaxTotalConnections(50);
		host.setMaxPerRouteConnections(10);
		host.setTimeToLive(10);
		host.setTimeUnit(TimeUnit.SECONDS);
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(
				new HashSet<>(Arrays.asList("zuul.host.maxTotalConnections",
						"zuul.host.maxPerRouteConnections", "zuul.host.timeToLive",
						"zuul.host.timeUnit")));
		filter.onApplicationEvent(event);

		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager) filter
				.getConnectionManager();
		assertThat(connMgr.getMaxTotal()).isEqualTo(50);
		assertThat(connMgr.getDefaultMaxPerRoute()).isEqualTo(10);
		Object pool = getField(connMgr, "pool");
		assertThat(pool).hasFieldOrPropertyWithValue("timeToLive", 10L);
		assertThat(pool).hasFieldOrPropertyWithValue("timeUnit", TimeUnit.SECONDS);
	}

	@Test
	public void getRequestBody() throws Exception {
		setupContext();
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/");
		request.setContent("{1}".getBytes());
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");
		RequestContext.getCurrentContext().setRequest(request);
		InputStream inputStream = getFilter().getRequestBody(request);
		assertThat(Arrays.equals("{1}".getBytes(), copyToByteArray(inputStream)))
				.isTrue();
	}

	@Test
	public void putRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(
				new ByteArrayInputStream(new byte[] { 1 }));
		HttpRequest httpRequest = getFilter().buildHttpRequest("PUT", "uri",
				inputStreamEntity, new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertThat(httpRequest instanceof HttpEntityEnclosingRequest).isTrue();
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertThat(httpEntityEnclosingRequest.getEntity() != null).isTrue();
	}

	@Test
	public void postRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(
				new ByteArrayInputStream(new byte[] { 1 }));
		HttpRequest httpRequest = getFilter().buildHttpRequest("POST", "uri",
				inputStreamEntity, new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertThat(httpRequest instanceof HttpEntityEnclosingRequest).isTrue();
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertThat(httpEntityEnclosingRequest.getEntity() != null).isTrue();
	}

	@Test
	public void pathRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(
				new ByteArrayInputStream(new byte[] { 1 }));
		HttpRequest httpRequest = getFilter().buildHttpRequest("PATCH", "uri",
				inputStreamEntity, new LinkedMultiValueMap<>(),
				new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		HttpPatch basicHttpRequest = (HttpPatch) httpRequest;
		assertThat(basicHttpRequest.getEntity() != null).isTrue();
	}

	@Test
	public void shouldFilterFalse() {
		setupContext();
		assertThat(getFilter().shouldFilter()).isEqualTo(false);
	}

	@Test
	public void shouldFilterTrue() throws Exception {
		setupContext();
		RequestContext.getCurrentContext().set("routeHost",
				new URL("http://localhost:8080"));
		RequestContext.getCurrentContext().set("sendZuulResponse", true);
		assertThat(getFilter().shouldFilter()).isEqualTo(true);
	}

	@Test
	public void filterOrder() {
		setupContext();
		assertThat(getFilter().filterOrder()).isEqualTo(100);
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
	}

	private SimpleHostRoutingFilter getFilter() {
		return this.context.getBean(SimpleHostRoutingFilter.class);
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class TestConfiguration {

		@Bean
		ZuulProperties zuulProperties() {
			return new ZuulProperties();
		}

		@Bean
		ApacheHttpClientFactory clientFactory() {
			return new DefaultApacheHttpClientFactory(HttpClientBuilder.create());
		}

		@Bean
		ApacheHttpClientConnectionManagerFactory connectionManagerFactory() {
			return new DefaultApacheHttpClientConnectionManagerFactory();
		}

		@Bean
		SimpleHostRoutingFilter simpleHostRoutingFilter(ZuulProperties zuulProperties,
				ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
				ApacheHttpClientFactory clientFactory) {
			return new SimpleHostRoutingFilter(new ProxyRequestHelper(zuulProperties),
					zuulProperties, connectionManagerFactory, clientFactory);
		}

	}

}
