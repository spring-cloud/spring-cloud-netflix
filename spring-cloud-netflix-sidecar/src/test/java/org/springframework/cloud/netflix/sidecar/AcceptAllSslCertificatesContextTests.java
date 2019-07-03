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

package org.springframework.cloud.netflix.sidecar;

import java.lang.reflect.Field;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;

import org.apache.http.config.Registry;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
		value = { "sidecar.accept-all-ssl-certificates=false" })
public class AcceptAllSslCertificatesContextTests {

	@Autowired
	RestTemplate restTemplate;

	@Test
	public void testUseRestTemplateWhenHttpClientIsNotAvailable() {
		HttpComponentsClientHttpRequestFactory requestFactory = (HttpComponentsClientHttpRequestFactory) restTemplate
				.getRequestFactory();
		CloseableHttpClient client = (CloseableHttpClient) requestFactory.getHttpClient();
		PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = getHttpClientConnectionManager(
				client);
		DefaultHttpClientConnectionOperator httpClientConnectionOperator = getHttpClientConnectionOperator(
				poolingHttpClientConnectionManager);
		Registry registry = getRegistry(httpClientConnectionOperator);
		Map registryMap = getRegistryMap(registry);
		SSLConnectionSocketFactory connectionSocketFactory = (SSLConnectionSocketFactory) registryMap
				.get("https");
		HostnameVerifier hostnameVerifier = getHostnameVerifier(connectionSocketFactory);
		assertThat(hostnameVerifier).isInstanceOf(DefaultHostnameVerifier.class);
	}

	private HostnameVerifier getHostnameVerifier(
			SSLConnectionSocketFactory connectionSocketFactory) {
		return getField(connectionSocketFactory, "hostnameVerifier");
	}

	private PoolingHttpClientConnectionManager getHttpClientConnectionManager(
			CloseableHttpClient httpClient) {
		return getField(httpClient, "connManager");
	}

	private DefaultHttpClientConnectionOperator getHttpClientConnectionOperator(
			PoolingHttpClientConnectionManager connectionManager) {
		return getField(connectionManager, "connectionOperator");
	}

	private Registry getRegistry(
			DefaultHttpClientConnectionOperator httpClientConnectionOperator) {
		return getField(httpClientConnectionOperator, "socketFactoryRegistry");
	}

	private Map getRegistryMap(Registry registry) {
		return getField(registry, "map");
	}

	private <T> T getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return (T) value;
	}

}
