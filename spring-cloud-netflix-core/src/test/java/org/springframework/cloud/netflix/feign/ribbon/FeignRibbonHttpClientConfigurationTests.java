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
package org.springframework.cloud.netflix.feign.ribbon;

import java.lang.reflect.Field;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.http.config.Lookup;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FeignRibbonHttpClientConfigurationTests.FeignRibbonHttpClientConfigurationTestsApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"debug=true","feign.httpclient.disableSslValidation=true"})
@DirtiesContext
public class FeignRibbonHttpClientConfigurationTests {

	@Autowired
	HttpClientConnectionManager connectionManager;

	@Test
	public void disableSslTest() throws Exception {
		Lookup<ConnectionSocketFactory> socketFactoryRegistry = getConnectionSocketFactoryLookup(connectionManager);
		assertNotNull(socketFactoryRegistry.lookup("https"));
		assertNull(this.getX509TrustManager(socketFactoryRegistry).getAcceptedIssuers());
	}

	private Lookup<ConnectionSocketFactory> getConnectionSocketFactoryLookup(HttpClientConnectionManager connectionManager) {
		DefaultHttpClientConnectionOperator connectionOperator = (DefaultHttpClientConnectionOperator)this.getField(connectionManager, "connectionOperator");
		return (Lookup)this.getField(connectionOperator, "socketFactoryRegistry");
	}

	private X509TrustManager getX509TrustManager(Lookup<ConnectionSocketFactory> socketFactoryRegistry) {
		ConnectionSocketFactory connectionSocketFactory = (ConnectionSocketFactory)socketFactoryRegistry.lookup("https");
		SSLSocketFactory sslSocketFactory = (SSLSocketFactory)this.getField(connectionSocketFactory, "socketfactory");
		SSLContextSpi sslContext = (SSLContextSpi)this.getField(sslSocketFactory, "context");
		return (X509TrustManager)this.getField(sslContext, "trustManager");
	}

	protected <T> Object getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return value;
	}

	@Configuration
	@EnableAutoConfiguration
	static class FeignRibbonHttpClientConfigurationTestsApplication {
		public static void main(String[] args) {
			new SpringApplicationBuilder(FeignRibbonClientRetryTests.Application.class)
					.run(args);
		}
	}
}
