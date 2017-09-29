package org.springframework.cloud.netflix.zuul.filters.route.apache;

import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
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
public class HttpClientRibbonCommandFactoryTest {

	SpringClientFactory springClientFactory;
	ZuulProperties zuulProperties;
	HttpClientRibbonCommandFactory ribbonCommandFactory;

	@Before
	public void setup(){
		this.springClientFactory = mock(SpringClientFactory.class);
		this.zuulProperties = new ZuulProperties();
		RibbonLoadBalancingHttpClient loadBalancingHttpClient = mock(RibbonLoadBalancingHttpClient.class);
		IClientConfig clientConfig = new DefaultClientConfigImpl();
		doReturn(loadBalancingHttpClient).when(this.springClientFactory).getClient(anyString(),
				eq(RibbonLoadBalancingHttpClient.class));
		doReturn(clientConfig).when(this.springClientFactory).getClientConfig(anyString());
		this.ribbonCommandFactory = new HttpClientRibbonCommandFactory(springClientFactory, zuulProperties, new HashSet<ZuulFallbackProvider>());
	}

	@Test
	public void testHystrixTimeoutValue() throws Exception {
		RibbonCommandContext context = mock(RibbonCommandContext.class);
		doReturn("service").when(context).getServiceId();
		HttpClientRibbonCommand ribbonCommand = this.ribbonCommandFactory.create(context);
		assertEquals(2000, ribbonCommand.getProperties().executionTimeoutInMilliseconds().get().intValue());
	}

}