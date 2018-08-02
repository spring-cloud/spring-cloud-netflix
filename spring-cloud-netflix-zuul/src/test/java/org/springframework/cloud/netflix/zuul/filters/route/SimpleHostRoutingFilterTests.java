/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.monitoring.CounterFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpPatch;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.util.StreamUtils.copyToByteArray;

/**
 * @author Andreas Kluth
 * @author Spencer Gibb
 * @author Gang Li
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
		TestPropertyValues.of("zuul.host.socket-timeout-millis=11000",
				"zuul.host.connect-timeout-millis=2100", "zuul.host.connection-request-timeout-millis=2500")
				.applyTo(this.context);
		setupContext();
		CloseableHttpClient httpClient = getFilter().newClient();
		Assertions.assertThat(httpClient).isInstanceOf(Configurable.class);
		RequestConfig config = ((Configurable) httpClient).getConfig();
		assertEquals(11000, config.getSocketTimeout());
		assertEquals(2100, config.getConnectTimeout());
		assertEquals(2500, config.getConnectionRequestTimeout());
	}

	@Test
	public void connectionPropertiesAreApplied() {
		TestPropertyValues.of("zuul.host.maxTotalConnections=100",
				"zuul.host.maxPerRouteConnections=10", "zuul.host.timeToLive=5",
				"zuul.host.timeUnit=SECONDS").applyTo(this.context);
		setupContext();
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager) getFilter().getConnectionManager();
		assertEquals(100, connMgr.getMaxTotal());
		assertEquals(10, connMgr.getDefaultMaxPerRoute());
		Object pool = getField(connMgr, "pool");
		Long timeToLive = getField(pool, "timeToLive");
		TimeUnit timeUnit = getField(pool, "tunit");
		assertEquals(new Long(5), timeToLive);
		assertEquals(TimeUnit.SECONDS, timeUnit);
	}

	protected <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

	@Test
	public void validateSslHostnamesByDefault() {
		setupContext();
		assertTrue("Hostname verification should be enabled by default",
				getFilter().isSslHostnameValidationEnabled());
	}

	@Test
	public void validationOfSslHostnamesCanBeDisabledViaProperty() {
		TestPropertyValues.of("zuul.sslHostnameValidationEnabled=false").applyTo(this.context);
		setupContext();
		assertFalse("Hostname verification should be disabled via property",
				getFilter().isSslHostnameValidationEnabled());
	}

	@Test
	public void defaultPropertiesAreApplied() {
		setupContext();
		PoolingHttpClientConnectionManager connMgr = (PoolingHttpClientConnectionManager) getFilter().getConnectionManager();

		assertEquals(200, connMgr.getMaxTotal());
		assertEquals(20, connMgr.getDefaultMaxPerRoute());
	}

	@Test
	public void deleteRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("DELETE", "uri", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertTrue(httpRequest instanceof HttpEntityEnclosingRequest);
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertTrue(httpEntityEnclosingRequest.getEntity() != null);
	}


	@Test
	public void zuulHostKeysUpdateHttpClient() {
		setupContext();
		SimpleHostRoutingFilter filter = getFilter();
		CloseableHttpClient httpClient = (CloseableHttpClient) ReflectionTestUtils.getField(filter, "httpClient");
		EnvironmentChangeEvent event = new EnvironmentChangeEvent(Collections.singleton("zuul.host.mykey"));
		filter.onPropertyChange(event);
		CloseableHttpClient newhttpClient = (CloseableHttpClient) ReflectionTestUtils.getField(filter, "httpClient");
		Assertions.assertThat(httpClient).isNotEqualTo(newhttpClient);
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
		assertTrue(Arrays.equals("{1}".getBytes(), copyToByteArray(inputStream)));
	}

	@Test
	public void putRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("PUT", "uri", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertTrue(httpRequest instanceof HttpEntityEnclosingRequest);
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertTrue(httpEntityEnclosingRequest.getEntity() != null);
	}

	@Test
	public void postRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("POST", "uri", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		assertTrue(httpRequest instanceof HttpEntityEnclosingRequest);
		HttpEntityEnclosingRequest httpEntityEnclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
		assertTrue(httpEntityEnclosingRequest.getEntity() != null);
	}

	@Test
	public void pathRequestBuiltWithBody() {
		setupContext();
		InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(new byte[]{1}));
		HttpRequest httpRequest = getFilter().buildHttpRequest("PATCH", "uri", inputStreamEntity,
				new LinkedMultiValueMap<>(), new LinkedMultiValueMap<>(), new MockHttpServletRequest());

		HttpPatch basicHttpRequest = (HttpPatch) httpRequest;
		assertTrue(basicHttpRequest.getEntity() != null);
	}

	@Test
	public void shouldFilterFalse() {
		setupContext();
		assertEquals(false, getFilter().shouldFilter());
	}

	@Test
	public void shouldFilterTrue() throws Exception {
		setupContext();
		RequestContext.getCurrentContext().set("routeHost", new URL("http://localhost:8080"));
		RequestContext.getCurrentContext().set("sendZuulResponse", true);
		assertEquals(true, getFilter().shouldFilter());
	}

	@Test
	public void filterOrder() {
		setupContext();
		assertEquals(100, getFilter().filterOrder());
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
			return new SimpleHostRoutingFilter(new ProxyRequestHelper(zuulProperties), zuulProperties, connectionManagerFactory, clientFactory);
		}
	}
}
