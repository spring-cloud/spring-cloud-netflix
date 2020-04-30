/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration.RefreshablePeerEurekaNodes;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Fahim Farook
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RefreshablePeerEurekaNodesTests.Application.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=eureka-server",
				"eureka.client.service-url.defaultZone=http://localhost:8678/eureka/" })
public class RefreshablePeerEurekaNodesTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private PeerEurekaNodes peerEurekaNodes;

	@Value("${local.server.port}")
	private int port = 0;

	private static final String DEFAULT_ZONE = "eureka.client.service-url.defaultZone";

	private static final String REGION = "eureka.client.region";

	private static final String USE_DNS = "eureka.client.use-dns-for-fetching-service-urls";

	@Test
	public void notUpdatedWhenDnsIsTrue() {
		changeProperty("eureka.client.use-dns-for-fetching-service-urls=true",
				"eureka.client.region=unavailable-region", // to force defaultZone
				"eureka.client.service-url.defaultZone=https://default-host1:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(
				new HashSet<>(Arrays.asList(USE_DNS, DEFAULT_ZONE))));

		assertThat(serviceUrlMatches("https://default-host1:8678/eureka/")).as(
				"PeerEurekaNodes' are updated when eureka.client.use-dns-for-fetching-service-urls is true")
				.isFalse();
	}

	@Test
	public void updatedWhenDnsIsFalse() {
		changeProperty("eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=unavailable-region", // to force defaultZone
				"eureka.client.service-url.defaultZone=https://default-host2:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(
				new HashSet<>(Arrays.asList(USE_DNS, DEFAULT_ZONE))));

		assertThat(serviceUrlMatches("https://default-host2:8678/eureka/")).as(
				"PeerEurekaNodes' are not updated when eureka.client.use-dns-for-fetching-service-urls is false")
				.isTrue();
	}

	@Test
	public void updatedWhenRegionChanged() {
		changeProperty("eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=region1",
				"eureka.client.availability-zones.region1=region1-zone",
				"eureka.client.availability-zones.region2=region2-zone",
				"eureka.client.service-url.region1-zone=https://region1-zone-host:8678/eureka/",
				"eureka.client.service-url.region2-zone=https://region2-zone-host:8678/eureka/");
		this.context
				.publishEvent(new EnvironmentChangeEvent(Collections.singleton(REGION)));
		assertThat(serviceUrlMatches("https://region1-zone-host:8678/eureka/")).as(
				"PeerEurekaNodes' are not updated when eureka.client.region is changed")
				.isTrue();

		changeProperty("eureka.client.region=region2");
		this.context
				.publishEvent(new EnvironmentChangeEvent(Collections.singleton(REGION)));
		assertThat(serviceUrlMatches("https://region2-zone-host:8678/eureka/")).as(
				"PeerEurekaNodes' are not updated when eureka.client.region is changed")
				.isTrue();
	}

	@Test
	public void updatedWhenAvailabilityZoneChanged() {
		changeProperty("eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=region4",
				"eureka.client.availability-zones.region3=region3-zone",
				"eureka.client.service-url.region4-zone=https://region4-zone-host:8678/eureka/",
				"eureka.client.service-url.defaultZone=https://default-host3:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(
				Collections.singleton("eureka.client.availability-zones.region3")));
		assertThat(this.peerEurekaNodes.getPeerEurekaNodes().get(0).getServiceUrl()
				.equals("https://default-host3:8678/eureka/")).isTrue();

		changeProperty("eureka.client.availability-zones.region4=region4-zone");
		this.context.publishEvent(new EnvironmentChangeEvent(
				Collections.singleton("eureka.client.availability-zones.region4")));
		assertThat(serviceUrlMatches("https://region4-zone-host:8678/eureka/")).as(
				"PeerEurekaNodes' are not updated when eureka.client.availability-zones are changed")
				.isTrue();
	}

	@Test
	public void notUpdatedWhenIrrelevantPropertiesChanged() {
		// Only way to test this is verifying whether updatePeerEurekaNodes() is invoked.

		// PeerEurekaNodes.updatePeerEurekaNodes() is not public, hence cannot verify with
		// Mockito.
		class VerifyablePeerEurekNode extends RefreshablePeerEurekaNodes {

			VerifyablePeerEurekNode(PeerAwareInstanceRegistry registry,
					EurekaServerConfig serverConfig, EurekaClientConfig clientConfig,
					ServerCodecs serverCodecs,
					ApplicationInfoManager applicationInfoManager) {
				super(registry, serverConfig, clientConfig, serverCodecs,
						applicationInfoManager,
						new ReplicationClientAdditionalFilters(Collections.emptySet()));
			}

			protected void updatePeerEurekaNodes(List<String> newPeerUrls) {
				super.updatePeerEurekaNodes(newPeerUrls);
			}

		}

		// Create stubs.
		final EurekaClientConfigBean configClientBean = mock(
				EurekaClientConfigBean.class);
		when(configClientBean.isUseDnsForFetchingServiceUrls()).thenReturn(false);
		final VerifyablePeerEurekNode mock = spy(
				new VerifyablePeerEurekNode(null, null, configClientBean, null, null));

		mock.onApplicationEvent(new EnvironmentChangeEvent(
				Collections.singleton("some.irrelevant.property")));
		verify(mock, never()).updatePeerEurekaNodes(anyList());
	}

	@Test
	public void peerEurekaNodesIsRefreshablePeerEurekaNodes() {
		assertThat(this.peerEurekaNodes).isNotNull();
		assertThat(this.peerEurekaNodes instanceof RefreshablePeerEurekaNodes)
				.as("PeerEurekaNodes should be an instance of RefreshablePeerEurekaNodes")
				.isTrue();
	}

	@Test
	public void serviceUrlsCountAsSoonAsRefreshed() {
		changeProperty(
				"eureka.client.service-url.defaultZone=https://defaul-host3:8678/eureka/,http://defaul-host4:8678/eureka/");
		forceUpdate();
		assertThat(this.peerEurekaNodes.getPeerEurekaNodes().size())
				.as("PeerEurekaNodes' peer count is incorrect.").isEqualTo(2);
	}

	@Test
	public void serviceUrlsValueAsSoonAsRefreshed() {
		changeProperty(
				"eureka.client.service-url.defaultZone=https://defaul-host4:8678/eureka/");
		forceUpdate();
		assertThat(serviceUrlMatches("https://defaul-host4:8678/eureka/"))
				.as("PeerEurekaNodes' new peer[0] is incorrect").isTrue();
	}

	@Test
	public void dashboardUpdatedAsSoonAsRefreshed() {
		changeProperty(
				"eureka.client.service-url.defaultZone=https://defaul-host5:8678/eureka/");
		forceUpdate();
		final ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		final String body = entity.getBody();
		assertThat(body).isNotNull();
		assertThat(body.contains("https://defaul-host5:8678/eureka/"))
				.as("DS Replicas not updated in the Eureka Server dashboard").isTrue();
	}

	@Test
	public void notUpdatedForRelaxedKeys() {
		changeProperty("eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=unavailable-region", // to force defaultZone
				"eureka.client.service-url.defaultZone=https://defaul-host6:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(
				Collections.singleton("eureka.client.serviceUrl.defaultZone")));
		assertThat(serviceUrlMatches("https://defaul-host6:8678/eureka/"))
				.as("PeerEurekaNodes' are updated for keys with relaxed binding")
				.isFalse();
	}

	/*
	 * Changes the value of given key in the environment.
	 */
	private void changeProperty(final String... pairs) {
		TestPropertyValues.of(pairs).applyTo(this.context);
	}

	/*
	 * Refreshes the context with properties satisfying to invoke update.
	 */
	private void forceUpdate() {
		changeProperty("eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=unavailable-region"); // to force defaultZone
		this.context.publishEvent(new EnvironmentChangeEvent(
				Collections.singleton("eureka.client.service-url.defaultZone")));
	}

	/*
	 * Whether the first element in PeerEurekaNodes matches the given url.
	 */
	private boolean serviceUrlMatches(final String serviceUrl) {
		return this.peerEurekaNodes.getPeerEurekaNodes().get(0).getServiceUrl()
				.equals(serviceUrl);
	}

	@EnableEurekaServer
	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class })
	protected static class Application {

	}

}
