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
package org.springframework.cloud.netflix.feign;

import okhttp3.OkHttpClient;

import java.lang.reflect.Field;
import javax.net.ssl.HostnameVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.ClassPathExclusions;
import org.springframework.cloud.FilteredClassPathRunner;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * @author Ryan Baxter
 */
@RunWith(FilteredClassPathRunner.class)
@ClassPathExclusions({ "ribbon-loadbalancer-{version:\\d.*}.jar" })
public class FeignOkHttpConfigurationTests {

	private ConfigurableApplicationContext context;

	@Before
	public void setUp() {
		context = new SpringApplicationBuilder().properties("debug=true","feign.httpclient.disableSslValidation=true",
				"feign.okhttp.enabled=true", "feign.httpclient.enabled=false").web(false)
				.sources(HttpClientConfiguration.class, FeignAutoConfiguration.class).run();
	}

	@After
	public void tearDown() {
		if(context != null) {
			context.close();
		}
	}

	@Test
	public void disableSslTest() throws Exception {
		OkHttpClient httpClient = context.getBean(OkHttpClient.class);
		HostnameVerifier hostnameVerifier = (HostnameVerifier)this.getField(httpClient, "hostnameVerifier");
		Assert.assertTrue(OkHttpClientFactory.TrustAllHostnames.class.isInstance(hostnameVerifier));
	}

	protected <T> Object getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return value;
	}
}
