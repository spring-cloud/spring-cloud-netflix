package org.springframework.cloud.netflix.feign.ribbon;

import static com.netflix.client.config.CommonClientConfigKey.ConnectTimeout;
import static com.netflix.client.config.CommonClientConfigKey.IsSecure;
import static com.netflix.client.config.CommonClientConfigKey.MaxAutoRetries;
import static com.netflix.client.config.CommonClientConfigKey.MaxAutoRetriesNextServer;
import static com.netflix.client.config.CommonClientConfigKey.OkToRetryOnAllOperations;
import static com.netflix.client.config.CommonClientConfigKey.ReadTimeout;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER;
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
	private IClientConfig config;

	private RibbonLoadBalancer ribbonLoadBalancer;

	private Integer defaultConnectTimeout = 10000;
	private Integer defaultReadTimeout = 10000;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(config.get(MaxAutoRetries, DEFAULT_MAX_AUTO_RETRIES)).thenReturn(1);
		when(config.get(MaxAutoRetriesNextServer, DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER))
				.thenReturn(1);
		when(config.get(OkToRetryOnAllOperations, eq(anyBoolean()))).thenReturn(true);
		when(config.get(ConnectTimeout)).thenReturn(defaultConnectTimeout);
		when(config.get(ReadTimeout)).thenReturn(defaultReadTimeout);
	}

	@Test
	@SneakyThrows
	public void testUriInsecure() {
		when(config.get(IsSecure)).thenReturn(false);
		ribbonLoadBalancer = new RibbonLoadBalancer(delegate, lb, config);
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		RibbonRequest ribbonRequest = new RibbonRequest(request, new URI(request.url()));

		Response response = Response.create(200, "Test",
				Collections.<String, Collection<String>> emptyMap(), new byte[0]);
		when(delegate.execute(any(Request.class), any(Options.class))).thenReturn(
				response);

		RibbonResponse resp = ribbonLoadBalancer.execute(ribbonRequest, null);

		assertThat(resp.getRequestedURI(), is(new URI("http://foo/")));
	}

	@Test
	@SneakyThrows
	public void testSecureUriFromClientConfig() {
		when(config.get(IsSecure)).thenReturn(true);
		ribbonLoadBalancer = new RibbonLoadBalancer(delegate, lb, config);
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		RibbonRequest ribbonRequest = new RibbonRequest(request, new URI(request.url()));

		Response response = Response.create(200, "Test",
				Collections.<String, Collection<String>> emptyMap(), new byte[0]);
		when(delegate.execute(any(Request.class), any(Options.class))).thenReturn(
				response);

		RibbonResponse resp = ribbonLoadBalancer.execute(ribbonRequest, null);

		assertThat(resp.getRequestedURI(), is(new URI("https://foo/")));
	}

	@Test
	@SneakyThrows
	public void testSecureUriFromClientConfigOverride() {
		when(config.get(IsSecure)).thenReturn(true);
		ribbonLoadBalancer = new RibbonLoadBalancer(delegate, lb, config);
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		RibbonRequest ribbonRequest = new RibbonRequest(request, new URI(request.url()));

		Response response = Response.create(200, "Test",
				Collections.<String, Collection<String>> emptyMap(), new byte[0]);
		when(delegate.execute(any(Request.class), any(Options.class))).thenReturn(
				response);

		IClientConfig override = mock(IClientConfig.class);
		when(override.get(ConnectTimeout, defaultConnectTimeout)).thenReturn(5000);
		when(override.get(ReadTimeout, defaultConnectTimeout)).thenReturn(5000);
		/*
		 * Override secure value.
		 */
		when(override.get(IsSecure)).thenReturn(false);

		RibbonResponse resp = ribbonLoadBalancer.execute(ribbonRequest, override);

		assertThat(resp.getRequestedURI(), is(new URI("http://foo/")));
	}
}
