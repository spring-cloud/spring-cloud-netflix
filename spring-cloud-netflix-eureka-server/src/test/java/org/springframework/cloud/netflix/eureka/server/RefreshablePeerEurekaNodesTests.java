/*
 * Copyright 2013-2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.eureka.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Ignore;
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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Fahim Farook
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
	classes = RefreshablePeerEurekaNodesTests.Application.class, 
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
	value = {
		"spring.application.name=eureka-server",
		"eureka.client.service-url.defaultZone=http://localhost:8678/eureka/" 
	})
public class RefreshablePeerEurekaNodesTests {

	@Autowired
	private ConfigurableApplicationContext context;
	
	@Autowired
	private PeerEurekaNodes peerEurekaNodes;
	
	@Value("${local.server.port}")
	private int port = 0;
	
	private	static final String DEFAULT_ZONE = "eureka.client.service-url.defaultZone";
	private	static final String REGION = "eureka.client.region";
	private	static final String USE_DNS = "eureka.client.use-dns-for-fetching-service-urls";
	
	@Test
	public void notUpdatedWhenDnsIsTrue() {		
		changeProperty(
				"eureka.client.use-dns-for-fetching-service-urls=true",
				"eureka.client.region=unavailable-region", // to force defaultZone 
				"eureka.client.service-url.defaultZone=http://default-host1:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(new HashSet<String>(Arrays.asList(USE_DNS, DEFAULT_ZONE))));
		
		assertFalse("PeerEurekaNodes' are updated when eureka.client.use-dns-for-fetching-service-urls is true",
				   serviceUrlMatches("http://default-host1:8678/eureka/"));
	}
	
	@Test
	public void updatedWhenDnsIsFalse() {
		changeProperty(
				"eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=unavailable-region", // to force defaultZone
				"eureka.client.service-url.defaultZone=http://default-host2:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(new HashSet<String>(Arrays.asList(USE_DNS, DEFAULT_ZONE))));
		
		assertTrue("PeerEurekaNodes' are not updated when eureka.client.use-dns-for-fetching-service-urls is false",
				   serviceUrlMatches("http://default-host2:8678/eureka/"));
	}
	
	
	@Test
	public void updatedWhenRegionChanged() {
		changeProperty(
				"eureka.client.use-dns-for-fetching-service-urls=false", 
				"eureka.client.region=region1",
				"eureka.client.availability-zones.region1=region1-zone",
				"eureka.client.availability-zones.region2=region2-zone",
				"eureka.client.service-url.region1-zone=http://region1-zone-host:8678/eureka/",
				"eureka.client.service-url.region2-zone=http://region2-zone-host:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(Collections.singleton(REGION)));
		assertTrue("PeerEurekaNodes' are not updated when eureka.client.region is changed",
				   serviceUrlMatches("http://region1-zone-host:8678/eureka/"));
		
		changeProperty("eureka.client.region=region2");
		this.context.publishEvent(new EnvironmentChangeEvent(Collections.singleton(REGION)));
		assertTrue("PeerEurekaNodes' are not updated when eureka.client.region is changed",
				   serviceUrlMatches("http://region2-zone-host:8678/eureka/"));
	}
	
	@Test
	public void updatedWhenAvailabilityZoneChanged() {
		changeProperty(
				"eureka.client.use-dns-for-fetching-service-urls=false", 
				"eureka.client.region=region4",
				"eureka.client.availability-zones.region3=region3-zone",
				"eureka.client.service-url.region4-zone=http://region4-zone-host:8678/eureka/",
				"eureka.client.service-url.defaultZone=http://default-host3:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(Collections.singleton("eureka.client.availability-zones.region3")));
		assertTrue(this.peerEurekaNodes.getPeerEurekaNodes().get(0).getServiceUrl().equals("http://default-host3:8678/eureka/"));
		
		changeProperty("eureka.client.availability-zones.region4=region4-zone");
		this.context.publishEvent(new EnvironmentChangeEvent(Collections.singleton("eureka.client.availability-zones.region4")));
		assertTrue("PeerEurekaNodes' are not updated when eureka.client.availability-zones are changed",
				   serviceUrlMatches("http://region4-zone-host:8678/eureka/"));
	}
		
	@Test
	public void notUpdatedWhenIrrelevantPropertiesChanged() {
		// Only way to test this is verifying whether updatePeerEurekaNodes() is invoked.
		
		// PeerEurekaNodes.updatePeerEurekaNodes() is not public, hence cannot verify with Mockito.
		class VerifyablePeerEurekNode extends RefreshablePeerEurekaNodes {
			public VerifyablePeerEurekNode(PeerAwareInstanceRegistry registry, EurekaServerConfig serverConfig,
										   EurekaClientConfig clientConfig, ServerCodecs serverCodecs,
										   ApplicationInfoManager applicationInfoManager) {
				super(registry, serverConfig, clientConfig, serverCodecs, applicationInfoManager);
			}

			protected void updatePeerEurekaNodes(List<String> newPeerUrls) {
				super.updatePeerEurekaNodes(newPeerUrls);
			}
		}
		
		// Create stubs.
		final EurekaClientConfigBean configClientBean = mock(EurekaClientConfigBean.class);
		when(configClientBean.isUseDnsForFetchingServiceUrls()).thenReturn(false);
		final VerifyablePeerEurekNode mock = spy(new VerifyablePeerEurekNode(null, null, configClientBean, null, null));		
		
		mock.onApplicationEvent(new EnvironmentChangeEvent(Collections.singleton("some.irrelevant.property")));	
		verify(mock, never()).updatePeerEurekaNodes(anyListOf(String.class));
	}
	
	@Test
	public void peerEurekaNodesIsRefreshablePeerEurekaNodes() {
		assertNotNull(this.peerEurekaNodes);
		assertTrue("PeerEurekaNodes should be an instance of RefreshablePeerEurekaNodes",
				   this.peerEurekaNodes instanceof RefreshablePeerEurekaNodes);
	}
	
	
	@Test
	public void serviceUrlsCountAsSoonAsRefreshed() {
		changeProperty("eureka.client.service-url.defaultZone=http://defaul-host3:8678/eureka/,http://defaul-host4:8678/eureka/");
		forceUpdate();
		assertThat("PeerEurekaNodes' peer count is incorrect.", 
				   this.peerEurekaNodes.getPeerEurekaNodes().size(), is(2));
	}
	
	
	@Test
	public void serviceUrlsValueAsSoonAsRefreshed() {
		changeProperty("eureka.client.service-url.defaultZone=http://defaul-host4:8678/eureka/");
		forceUpdate();
		assertTrue("PeerEurekaNodes' new peer[0] is incorrect",
				   serviceUrlMatches("http://defaul-host4:8678/eureka/"));
	}
	
	@Test
	public void dashboardUpdatedAsSoonAsRefreshed() {
		changeProperty("eureka.client.service-url.defaultZone=http://defaul-host5:8678/eureka/");
		forceUpdate();
		final ResponseEntity<String> entity = new TestRestTemplate()
				.getForEntity("http://localhost:" + this.port + "/", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		final String body = entity.getBody();
		assertNotNull(body);
		assertTrue("DS Replicas not updated in the Eureka Server dashboard", 
				   body.contains("http://defaul-host5:8678/eureka/"));
	}
	
	@Test
	public void notUpdatedForRelaxedKeys() {
		changeProperty(
				"eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=unavailable-region", // to force defaultZone
				"eureka.client.service-url.defaultZone=http://defaul-host6:8678/eureka/");
		this.context.publishEvent(new EnvironmentChangeEvent(Collections.singleton("eureka.client.serviceUrl.defaultZone")));
		assertFalse("PeerEurekaNodes' are updated for keys with relaxed binding",
				   serviceUrlMatches("http://defaul-host6:8678/eureka/"));
	}
	
	@EnableEurekaServer
	@Configuration
	@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class
			})
	protected static class Application {
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
		changeProperty(
				"eureka.client.use-dns-for-fetching-service-urls=false",
				"eureka.client.region=unavailable-region"); // to force defaultZone
		this.context.publishEvent(
				new EnvironmentChangeEvent(Collections.singleton("eureka.client.service-url.defaultZone")));
	}
	
	/*
	 * Whether the first element in PeerEurekaNodes matches the given url.
	 */
	private boolean serviceUrlMatches(final String serviceUrl) {
		return this.peerEurekaNodes.getPeerEurekaNodes().get(0).getServiceUrl().equals(serviceUrl);
	}
}
