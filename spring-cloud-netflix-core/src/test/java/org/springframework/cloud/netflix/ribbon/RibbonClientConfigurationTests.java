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

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;

import static org.hamcrest.Matchers.equalTo;
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
        config = new CountingConfig();
        config.setProperty(CommonClientConfigKey.ConnectTimeout, "1");
        config.setProperty(CommonClientConfigKey.ReadTimeout, "1");
        config.setProperty(CommonClientConfigKey.MaxHttpConnectionsPerHost, "1");
        config.setClientName("testClient");
    }

    @Test
    public void restClientInitCalledOnce() {
        new TestRestClient(config);
        assertThat(config.count, is(equalTo(1)));
    }

    static class CountingConfig extends DefaultClientConfigImpl {
        int count = 0;
    }

    @Test
    public void testSecureUriFromClientConfig() throws Exception {
        Server server = new Server("foo", 7777);
        when(inspector.isSecure(server)).thenReturn(true);

        RibbonClientConfiguration.OverrideRestClient overrideRestClient = new RibbonClientConfiguration.OverrideRestClient(this.config, inspector);
        URI uri = overrideRestClient.reconstructURIWithServer(server, new URI("http://foo/"));
        assertThat(uri, is(new URI("https://foo:7777/")));
    }

    @Test
    public void testInSecureUriFromClientConfig() throws Exception {
        Server server = new Server("foo", 7777);
        when(inspector.isSecure(server)).thenReturn(false);

        RibbonClientConfiguration.OverrideRestClient overrideRestClient = new RibbonClientConfiguration.OverrideRestClient(this.config, inspector);
        URI uri = overrideRestClient.reconstructURIWithServer(server, new URI("http://foo/"));
        assertThat(uri, is(new URI("http://foo:7777/")));
    }

    @Test
    public void testNotDoubleEncodedWhenSecure() throws Exception {
        Server server = new Server("foo", 7777);
        when(inspector.isSecure(server)).thenReturn(true);

        RibbonClientConfiguration.OverrideRestClient overrideRestClient = new RibbonClientConfiguration.OverrideRestClient(this.config, inspector);
        URI uri = overrideRestClient.reconstructURIWithServer(server, new URI("http://foo/%20bar"));
        assertThat(uri, is(new URI("https://foo:7777/%20bar")));
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
