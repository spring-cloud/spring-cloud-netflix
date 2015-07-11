package org.springframework.cloud.netflix.feign.ribbon;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import lombok.SneakyThrows;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.netflix.feign.ribbon.RibbonLoadBalancer.RibbonRequest;
import org.springframework.cloud.netflix.feign.ribbon.RibbonLoadBalancer.RibbonResponse;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.RequestTemplate;
import feign.Response;

public class RibbonLoadBalancerTests {
	
	@Mock
	private Client delegate;
	@Mock
	private ILoadBalancer lb;
	@Mock
	private IClientConfig clientConfig;
	
	private RibbonLoadBalancer ribbonLoadBalancer;
	
	private Integer defaultConnectTimeout = 10000;
	private Integer defaultReadTimeout = 10000;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(clientConfig.get(CommonClientConfigKey.MaxAutoRetries, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES)).thenReturn(1);
		when(clientConfig.get(CommonClientConfigKey.MaxAutoRetriesNextServer, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER)).thenReturn(1);
		when(clientConfig.get(CommonClientConfigKey.OkToRetryOnAllOperations, eq(anyBoolean()))).thenReturn(true);
		when(clientConfig.get(CommonClientConfigKey.ConnectTimeout)).thenReturn(defaultConnectTimeout);
		when(clientConfig.get(CommonClientConfigKey.ReadTimeout)).thenReturn(defaultReadTimeout);
	}
	
	@Test
	@SneakyThrows
	public void testUriInsecure() {
		when(clientConfig.get(CommonClientConfigKey.IsSecure)).thenReturn(false);
		ribbonLoadBalancer = new RibbonLoadBalancer(delegate, lb, clientConfig);
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		RibbonRequest ribbonRequest = new RibbonRequest(request, new URI(request.url()));
		
		Response response = Response.create(200, "Test", Collections.<String, Collection<String>> emptyMap(), new byte[0]);
		when(delegate.execute(any(Request.class), any(Options.class))).thenReturn(response);
		
		RibbonResponse resp = ribbonLoadBalancer.execute(ribbonRequest, null);
		
		assertThat(resp.getRequestedURI(), is(new URI("http://foo/")));
	}
	
	@Test
	@SneakyThrows
	public void testSecureUriFromClientConfig() {
		when(clientConfig.get(CommonClientConfigKey.IsSecure)).thenReturn(true);
		ribbonLoadBalancer = new RibbonLoadBalancer(delegate, lb, clientConfig);
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		RibbonRequest ribbonRequest = new RibbonRequest(request, new URI(request.url()));
		
		Response response = Response.create(200, "Test", Collections.<String, Collection<String>> emptyMap(), new byte[0]);
		when(delegate.execute(any(Request.class), any(Options.class))).thenReturn(response);
		
		RibbonResponse resp = ribbonLoadBalancer.execute(ribbonRequest, null);
		
		assertThat(resp.getRequestedURI(), is(new URI("https://foo/")));
	}
	
	@Test
	@SneakyThrows
	public void testSecureUriFromClientConfigOverride() {
		when(clientConfig.get(CommonClientConfigKey.IsSecure)).thenReturn(true);
		ribbonLoadBalancer = new RibbonLoadBalancer(delegate, lb, clientConfig);
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		RibbonRequest ribbonRequest = new RibbonRequest(request, new URI(request.url()));
		
		Response response = Response.create(200, "Test", Collections.<String, Collection<String>> emptyMap(), new byte[0]);
		when(delegate.execute(any(Request.class), any(Options.class))).thenReturn(response);
		
		IClientConfig override = mock(IClientConfig.class);
		when(override.get(CommonClientConfigKey.ConnectTimeout, defaultConnectTimeout)).thenReturn(5000);
		when(override.get(CommonClientConfigKey.ReadTimeout, defaultConnectTimeout)).thenReturn(5000);
		/*
		 * Override secure value.
		 */
		when(override.get(CommonClientConfigKey.IsSecure)).thenReturn(false);
		
		RibbonResponse resp = ribbonLoadBalancer.execute(ribbonRequest, override);
		
		assertThat(resp.getRequestedURI(), is(new URI("http://foo/")));
	}
}
