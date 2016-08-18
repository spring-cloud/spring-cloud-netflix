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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration.OverrideRestClient;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 */
public class RibbonClientConfigurationTests {

	private CountingConfig config;

	@Mock
	private ServerIntrospector introspector;

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
		when(this.introspector.isSecure(server)).thenReturn(true);

		OverrideRestClient overrideRestClient = new OverrideRestClient(this.config,
				this.introspector);
		URI uri = overrideRestClient.reconstructURIWithServer(server,
				new URI("http://foo/"));
		assertThat(uri, is(new URI("https://foo:7777/")));
	}

	@Test
	public void testInsecureUriFromClientConfig() throws Exception {
		Server server = new Server("foo", 7777);
		when(this.introspector.isSecure(server)).thenReturn(false);

		OverrideRestClient overrideRestClient = new OverrideRestClient(this.config,
				this.introspector);
		URI uri = overrideRestClient.reconstructURIWithServer(server,
				new URI("http://foo/"));
		assertThat(uri, is(new URI("http://foo:7777/")));
	}

	@Test
	public void testNotDoubleEncodedWhenSecure() throws Exception {
		Server server = new Server("foo", 7777);
		when(this.introspector.isSecure(server)).thenReturn(true);

		OverrideRestClient overrideRestClient = new OverrideRestClient(this.config,
				this.introspector);
		URI uri = overrideRestClient.reconstructURIWithServer(server,
				new URI("http://foo/%20bar"));
		assertThat(uri, is(new URI("https://foo:7777/%20bar")));
	}

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
