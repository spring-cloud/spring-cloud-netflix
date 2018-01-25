package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import java.util.HashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;

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
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(this.springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(this.springClientFactory).getClientConfig(anyString());
		commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
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
		assertEquals(4000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
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
	public void testHystrixDefaultAndRibbonSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 30);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(30, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testHystrixCommandAndRibbonSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 30);
		ConfigurationManager.getConfigInstance().setProperty("hystrix.command.service.execution.isolation.thread.timeoutInMilliseconds", 50);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(50, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testDefaultRibbonSetting() throws Exception {
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(4000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testRibbonAndRibbonRetriesDefaultSetting() throws Exception {
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
				eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(1200, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testRibbonTimeoutAndRibbonRetriesDefaultAndNameSpaceSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("service.test.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.test.ReadTimeout", 1000);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl("test");
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(4000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testRibbonTimeoutAndRibbonRetriesDefaultAndDefaultSpaceSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout", 1000);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(4000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testRibbonTimeoutAndRibbonNameSpaceRetriesDefaultAndDefaultSpaceSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.test.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl("test");
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(1800, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testRibbonRetriesAndRibbonTimeoutSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(3600, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testRibbonCommandRetriesAndRibbonCommandTimeoutSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ReadTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetries", 1);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetriesNextServer", 2);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(12000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

	@Test
	public void testRibbonCommandRetriesAndRibbonCommandTimeoutPartOfSetting() throws Exception {
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.ConnectTimeout", 1000);
		ConfigurationManager.getConfigInstance().setProperty("service.ribbon.MaxAutoRetries", 1);
		SpringClientFactory springClientFactory = mock(SpringClientFactory.class);
		ZuulProperties zuulProperties = new ZuulProperties();
		OkHttpLoadBalancingClient loadBalancingHttpClient = mock(OkHttpLoadBalancingClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		clientConfig.set(IClientConfigKey.Keys.ConnectTimeout, 100);
		clientConfig.set(IClientConfigKey.Keys.ReadTimeout, 500);
		doReturn(loadBalancingHttpClient).when(springClientFactory).getClient(anyString(),
			eq(OkHttpLoadBalancingClient.class));
		doReturn(clientConfig).when(springClientFactory).getClientConfig(anyString());
		OkHttpRibbonCommandFactory commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = commandFactory.create(context);
		assertEquals(6000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

}