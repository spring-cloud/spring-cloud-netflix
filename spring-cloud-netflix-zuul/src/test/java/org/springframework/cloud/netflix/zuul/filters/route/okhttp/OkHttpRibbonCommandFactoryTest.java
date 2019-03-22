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

package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import java.util.HashSet;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 * @author Gang Li
 */
public class OkHttpRibbonCommandFactoryTest {

	SpringClientFactory springClientFactory;

	ZuulProperties zuulProperties;

	OkHttpRibbonCommandFactory commandFactory;

	@Before
	public void setup() {
		this.springClientFactory = mock(SpringClientFactory.class);
		this.zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(this.springClientFactory)
				.getClient(anyString(), eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(this.springClientFactory)
				.getClientConfig(anyString());
		commandFactory = new OkHttpRibbonCommandFactory(springClientFactory,
				zuulProperties, new HashSet<FallbackProvider>());
	}

	@After
	public void after() {
		ConfigurationManager.getConfigInstance().clear();
		HystrixPropertiesFactory.reset();
	}

	@Test
	public void testHystrixTimeoutValue() throws Exception {
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(4000);
	}

	@Test
	public void testHystrixTimeoutValueSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty(
				"hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",
				50);
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(50);
	}

	@Test
	public void testHystrixTimeoutValueCommandSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty(
				"hystrix.command.service.execution.isolation.thread.timeoutInMilliseconds",
				50);
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(50);
	}

	@Test
	public void testHystrixTimeoutValueCommandAndDefaultSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty(
				"hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",
				30);
		ConfigurationManager.getConfigInstance().setProperty(
				"hystrix.command.service.execution.isolation.thread.timeoutInMilliseconds",
				50);
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(50);
	}

	@Test
	public void testHystrixDefaultAndRibbonSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty(
				"hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",
				30);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout",
				1000);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(30);
	}

	@Test
	public void testHystrixCommandAndRibbonSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty(
				"hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",
				30);
		ConfigurationManager.getConfigInstance().setProperty(
				"hystrix.command.service.execution.isolation.thread.timeoutInMilliseconds",
				50);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout",
				1000);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(50);
	}

	@Test
	public void testDefaultRibbonSetting() throws Exception {
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(4000);
	}

	@Test
	public void testRibbonAndRibbonRetriesDefaultSetting() throws Exception {
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(1200);
	}

	@Test
	public void testRibbonTimeoutAndRibbonRetriesDefaultAndNameSpaceSetting()
			throws Exception {
		ConfigurationManager.getConfigInstance()
				.setProperty("service.test.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.test.ReadTimeout",
				1000);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl("test");
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(4000);
	}

	@Test
	public void testRibbonTimeoutAndRibbonRetriesDefaultAndDefaultSpaceSetting()
			throws Exception {
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout",
				1000);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(4000);
	}

	@Test
	public void testRibbonTimeoutAndRibbonNameSpaceRetriesDefaultAndDefaultSpaceSetting()
			throws Exception {
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout",
				1000);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.test.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl("test");
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(1800);
	}

	@Test
	public void testRibbonRetriesAndRibbonTimeoutSetting() throws Exception {
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(3600);
	}

	@Test
	public void testRibbonCommandRetriesAndRibbonCommandTimeoutSetting()
			throws Exception {
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout",
				1000);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(12000);
	}

	@Test
	public void testRibbonCommandRetriesAndRibbonCommandTimeoutPartOfSetting()
			throws Exception {
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance()
				.setProperty("service.ribbon.MaxAutoRetries", 1);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(
				OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(
				springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertThat(ribbonCommand.getProperties().executionTimeoutInMilliseconds().get()
				.intValue()).isEqualTo(6000);
	}

}
