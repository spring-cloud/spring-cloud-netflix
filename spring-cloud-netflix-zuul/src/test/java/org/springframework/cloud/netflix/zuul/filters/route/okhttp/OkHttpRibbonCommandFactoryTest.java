package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import java.util.HashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 */
public class OkHttpRibbonCommandFactoryTest {

	SpringClientFactory springClientFactory;
	ZuulProperties zuulProperties;
	OkHttpRibbonCommandFactory commandFactory;

	@Before
	public void setup() {
		this.springClientFactory = mock(SpringClientFactory.class);
		this.zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(this.springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(this.springClientFactory).getClientConfig(anyString());
		commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
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
		assertEquals(2000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testHystrixTimeoutValueSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 50);
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertEquals(50, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testHystrixTimeoutValueCommandSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.service.execution.isolation.thread.timeoutInMilliseconds", 50);
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertEquals(50, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testHystrixTimeoutValueCommandAndDefaultSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 30);
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.service.execution.isolation.thread.timeoutInMilliseconds", 50);
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertEquals(50, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testHystrixTimeoutValueRibbonTimeouts() throws Exception {
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<FallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(600, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

}