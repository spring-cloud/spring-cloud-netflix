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

package org.springframework.cloud.netflix.feign.ribbon;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import feign.ribbon.LBClient;
import feign.ribbon.LBClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Spencer Gibb
 */
public class CachingLBClientFactoryTests {

	@Mock
	private LBClientFactory delegate;

	private CachingLBClientFactory factory;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);

		IClientConfig config = new DefaultClientConfigImpl();
		config.set(CommonClientConfigKey.ConnectTimeout, 1000);
		config.set(CommonClientConfigKey.ReadTimeout, 500);

		LBClient client1 = LBClient.create(null, config);
		LBClient client2 = LBClient.create(null, config);

		when(delegate.create("client1")).thenReturn(client1);
		when(delegate.create("client2")).thenReturn(client2);

		factory = new CachingLBClientFactory(delegate);
	}

	@Test
	public void delegateCreatesWhenMissing() {
		LBClient client = factory.create("client1");
		assertNotNull("client was null", client);

		verify(delegate, times(1)).create("client1");
	}

	@Test
	public void cacheWorks() {
		LBClient client = factory.create("client2");
		assertNotNull("client was null", client);

		client = factory.create("client2");
		assertNotNull("client was null", client);

		verify(delegate, times(1)).create("client2");
	}

}
