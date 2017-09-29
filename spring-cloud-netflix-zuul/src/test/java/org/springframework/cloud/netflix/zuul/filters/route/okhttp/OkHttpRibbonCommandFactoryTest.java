package org.springframework.cloud.netflix.zuul.filters.route.okhttp;

import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

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
		commandFactory = new OkHttpRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
	}

	@Test
	public void testHystrixTimeoutValue() throws Exception {
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		OkHttpRibbonCommand ribbonCommand = this.commandFactory.create(context);
		assertEquals(2000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

}