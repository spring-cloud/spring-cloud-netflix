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

import org.junit.Test;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Spencer Gibb
 */
public class RibbonClientConfigurationTests {

	@Test
	public void restClientInitCalledOnce() {
		CountingConfig config = new CountingConfig();
		config.setProperty(CommonClientConfigKey.ConnectTimeout, "1");
		config.setProperty(CommonClientConfigKey.ReadTimeout, "1");
		config.setProperty(CommonClientConfigKey.MaxHttpConnectionsPerHost, "1");
		config.setClientName("testClient");
		new TestRestClient(config);
		assertThat(config.count, is(equalTo(1)));
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
		assertThat(uri.getScheme(), is(equalTo("https")));
		assertThat(uri.getHost(), is(equalTo("example.com")));
	}

	static class CountingConfig extends DefaultClientConfigImpl {
		int count = 0;
	}

	static class TestRestClient extends RibbonClientConfiguration.OverrideRestClient {

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
