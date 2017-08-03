/*
 * Copyright 2013-2015 the original author or authors.
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
 */

package org.springframework.cloud.netflix.ribbon;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration.OverrideRestClient;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.client.http.RestClient;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 */
public class RibbonClientConfigurationTests {

	private CountingConfig config;

	@Mock
	private ServerIntrospector inspector;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.config = new CountingConfig();
		this.config.setProperty(CommonClientConfigKey.ConnectTimeout, "1");
		this.config.setProperty(CommonClientConfigKey.ReadTimeout, "1");
		this.config.setProperty(CommonClientConfigKey.MaxHttpConnectionsPerHost, "1");
		this.config.setClientName("testClient");
	}

	@Test
	public void restClientInitCalledOnce() {
		new TestRestClient(this.config);
		assertThat(this.config.count, is(1));
	}

	@Test
	public void restClientWithSecureServer() throws Exception {
		CountingConfig config = new CountingConfig();
		config.setProperty(CommonClientConfigKey.ConnectTimeout, "1");
		config.setProperty(CommonClientConfigKey.ReadTimeout, "1");
		config.setProperty(CommonClientConfigKey.MaxHttpConnectionsPerHost, "1");
		config.setClientName("bar");
		Server server = new Server("example.com", 443);
		URI uri = new TestRestClient(config).reconstructURIWithServer(server,
				new URI("/foo"));
		assertThat(uri.getScheme(), is("https"));
		assertThat(uri.getHost(), is("example.com"));
	}

	static class CountingConfig extends DefaultClientConfigImpl {
		int count = 0;
	}

	@Test
	public void testSecureUriFromClientConfig() throws Exception {
		Server server = new Server("foo", 7777);
		when(this.inspector.isSecure(server)).thenReturn(true);

		for (AbstractLoadBalancerAwareClient client : clients()) {
			URI uri = client.reconstructURIWithServer(server,
					new URI("http://foo/"));
			assertThat(getReason(client), uri, is(new URI("https://foo:7777/")));
		}
	}

	@Test
	public void testInSecureUriFromClientConfig() throws Exception {
		Server server = new Server("foo", 7777);
		when(this.inspector.isSecure(server)).thenReturn(false);

		for (AbstractLoadBalancerAwareClient client : clients()) {
			URI uri = client.reconstructURIWithServer(server,
					new URI("http://foo/"));
			assertThat(getReason(client), uri, is(new URI("http://foo:7777/")));
		}
	}

	String getReason(AbstractLoadBalancerAwareClient client) {
		return client.getClass().getSimpleName()+" failed";
	}

	@Test
	public void testNotDoubleEncodedWhenSecure() throws Exception {
		Server server = new Server("foo", 7777);
		when(this.inspector.isSecure(server)).thenReturn(true);

		for (AbstractLoadBalancerAwareClient client : clients()) {
			URI uri = client.reconstructURIWithServer(server,
					new URI("http://foo/%20bar"));
			assertThat(getReason(client), uri, is(new URI("https://foo:7777/%20bar")));
		}
	}

	@Test
	public void testPlusInQueryStringGetsRewrittenWhenServerIsSecure() throws Exception {
		Server server = new Server("foo", 7777);
		when(this.inspector.isSecure(server)).thenReturn(true);

		for (AbstractLoadBalancerAwareClient client : clients()) {
			URI uri = client.reconstructURIWithServer(server, new URI("http://foo/%20bar?hello=1+2"));
			assertThat(uri, is(new URI("https://foo:7777/%20bar?hello=1%202")));
		}
	}

	private List<AbstractLoadBalancerAwareClient> clients() {
		ArrayList<AbstractLoadBalancerAwareClient> clients = new ArrayList<>();
		clients.add(new OverrideRestClient(this.config, this.inspector));
		clients.add(new RibbonLoadBalancingHttpClient(this.config, this.inspector));
		clients.add(new OkHttpLoadBalancingClient(this.config, this.inspector));
		return clients;
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testDefaultsToApacheHttpClient() {
		testClient(RibbonLoadBalancingHttpClient.class, null, RestClient.class, OkHttpLoadBalancingClient.class);
		testClient(RibbonLoadBalancingHttpClient.class, new String[]{"ribbon.httpclient.enabled"}, RestClient.class, OkHttpLoadBalancingClient.class);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testEnableRestClient() {
		testClient(RestClient.class, new String[]{"ribbon.restclient.enabled"}, RibbonLoadBalancingHttpClient.class,
				OkHttpLoadBalancingClient.class);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testEnableOkHttpClient() {
		testClient(OkHttpLoadBalancingClient.class, new String[]{"ribbon.okhttp.enabled"}, RibbonLoadBalancingHttpClient.class,
				RestClient.class);
	}

	void testClient(Class<?> clientType, String[] properties, Class<?>... excludedTypes) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(HttpClientConfiguration.class, RibbonAutoConfiguration.class,
				RibbonClientConfiguration.class);
		if (properties != null) {
			EnvironmentTestUtils.addEnvironment(context, properties);
		}
		context.refresh();
		context.getBean(clientType);
		for (Class<?> excludedType : excludedTypes) {
			assertThat("has "+excludedType.getSimpleName()+ " instance", hasInstance(context, excludedType), is(false));
		}
		context.close();
	}

	private <T> boolean hasInstance(ListableBeanFactory lbf, Class<T> requiredType) {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(lbf,
				requiredType).length > 0;
	}

	@Configuration
	@EnableAutoConfiguration
	protected static class TestLBConfig { }

	static class TestRestClient extends OverrideRestClient {

		private TestRestClient(IClientConfig ncc) {
			super(ncc, new DefaultServerIntrospector());
		}

		@Override
		public void initWithNiwsConfig(IClientConfig clientConfig) {
			((CountingConfig) clientConfig).count++;
			super.initWithNiwsConfig(clientConfig);
		}
	}
}
