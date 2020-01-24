/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.metadata;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultManagementMetadataProviderTest {

	private static final EurekaInstanceConfigBean INSTANCE = mock(
			EurekaInstanceConfigBean.class);

	private final ManagementMetadataProvider provider = new DefaultManagementMetadataProvider();

	@Before
	public void setUp() throws Exception {
		when(INSTANCE.getHostname()).thenReturn("host");
		when(INSTANCE.getHealthCheckUrlPath()).thenReturn("health");
		when(INSTANCE.getStatusPageUrlPath()).thenReturn("info");
		when(INSTANCE.isSecurePortEnabled()).thenReturn(false);
	}

	@Test
	public void serverPortIsRandomAndManagementPortIsNull() throws Exception {
		int serverPort = 0;
		int secureEurekaPort = 0;
		String serverContextPath = "/";
		String managementContextPath = null;
		Integer managementPort = null;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual).isNull();
	}

	@Test
	public void managementPortIsRandom() throws Exception {
		int serverPort = 0;
		int secureEurekaPort = 0;
		String serverContextPath = "/";
		String managementContextPath = null;
		Integer managementPort = 0;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual).isNull();
	}

	@Test
	public void serverPort() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/";
		String managementContextPath = null;
		Integer managementPort = null;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl()).isEqualTo("http://host:7777/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl()).isEqualTo("http://host:7777/info");
		assertThat(actual.getManagementPort()).isEqualTo(7777);
	}

	@Test
	public void serverPortManagementPort() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/";
		String managementContextPath = null;
		Integer managementPort = 8888;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl()).isEqualTo("http://host:8888/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl()).isEqualTo("http://host:8888/info");
		assertThat(actual.getManagementPort()).isEqualTo(8888);
	}

	@Test
	public void serverPortManagementPortServerContextPath() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/Server";
		String managementContextPath = null;
		Integer managementPort = 8888;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl()).isEqualTo("http://host:8888/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl()).isEqualTo("http://host:8888/info");
		assertThat(actual.getManagementPort()).isEqualTo(8888);
	}

	@Test
	public void serverPortManagementPortServerContextPathManagementContextPath()
			throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/Server";
		String managementContextPath = "/Management";
		Integer managementPort = 8888;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl())
				.isEqualTo("http://host:8888/Management/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl())
				.isEqualTo("http://host:8888/Management/info");
		assertThat(actual.getManagementPort()).isEqualTo(8888);
	}

	@Test
	public void serverPortServerContextPathManagementContextPath() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/Server";
		String managementContextPath = "/Management";
		Integer managementPort = null;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl())
				.isEqualTo("http://host:7777/Server/Management/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl())
				.isEqualTo("http://host:7777/Server/Management/info");
		assertThat(actual.getManagementPort()).isEqualTo(7777);
	}

	@Test
	public void serverPortManagementContextPath() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/";
		String managementContextPath = "/Management";
		Integer managementPort = null;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl())
				.isEqualTo("http://host:7777/Management/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl())
				.isEqualTo("http://host:7777/Management/info");
		assertThat(actual.getManagementPort()).isEqualTo(7777);
	}

	@Test
	public void serverPortServerContextPath() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/Server";
		String managementContextPath = null;
		Integer managementPort = null;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl())
				.isEqualTo("http://host:7777/Server/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl()).isEqualTo("http://host:7777/Server/info");
		assertThat(actual.getManagementPort()).isEqualTo(7777);
	}

	@Test
	public void serverPortManagementPortManagementContextPath() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 0;
		String serverContextPath = "/";
		String managementContextPath = "/Management";
		Integer managementPort = 8888;
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl())
				.isEqualTo("http://host:8888/Management/health");
		assertThat(actual.getSecureHealthCheckUrl()).isNullOrEmpty();
		assertThat(actual.getStatusPageUrl())
				.isEqualTo("http://host:8888/Management/info");
		assertThat(actual.getManagementPort()).isEqualTo(8888);

	}

	@Test
	public void setSecureHealthCheckUrl() throws Exception {
		int serverPort = 7777;
		int secureEurekaPort = 7654;
		String serverContextPath = "/";
		String managementContextPath = "/Management";
		Integer managementPort = 8888;
		doReturn(true).when(INSTANCE).isSecurePortEnabled();
		ManagementMetadata actual = provider.get(INSTANCE, serverPort, serverContextPath,
				managementContextPath, managementPort, secureEurekaPort);

		assertThat(actual.getHealthCheckUrl())
				.isEqualTo("http://host:8888/Management/health");
		assertThat(actual.getSecureHealthCheckUrl())
				.isEqualTo("https://host:7654/Management/health");
		assertThat(actual.getStatusPageUrl())
				.isEqualTo("http://host:8888/Management/info");
		assertThat(actual.getManagementPort()).isEqualTo(8888);
	}

}
