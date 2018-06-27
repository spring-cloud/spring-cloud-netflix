/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import java.util.Collections;
import java.util.HashMap;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.netflix.appinfo.InstanceInfo.DEFAULT_PORT;
import static com.netflix.appinfo.InstanceInfo.DEFAULT_SECURE_PORT;
import static org.springframework.util.Assert.isTrue;

/**
 * Mocked Eureka Server
 * 
 * @author Daniel Lavoie
 */
@Configuration
@RestController
@SpringBootApplication
public class EurekaServerMockApplication {
	private static final InstanceInfo INFO = InstanceInfo.Builder.newBuilder()
			.setInstanceId("app1instance1")
			.setAppName("app1")
			.setAppNameForDeser("app1fordeser")
			.setAppGroupName("app1group")
			.setAppGroupNameForDeser("app1group1fordeser")
			.setHostName("app1host1")
			.setStatus(InstanceInfo.InstanceStatus.UP)
			.setOverriddenStatus(InstanceInfo.InstanceStatus.DOWN)
			.setIPAddr("127.0.0.1")
			.setSID("app1sid")
			.setPort(8080)
			.setSecurePort(4443)
			.enablePort(InstanceInfo.PortType.UNSECURE, true)
			.setHomePageUrl("/", "http://localhost/")
			.setHomePageUrlForDeser("http://localhost/")
			.setStatusPageUrl("/status", "http://localhost/info")
			.setStatusPageUrlForDeser("http://localhost/status")
			.setHealthCheckUrls("/ping", "http://localhost/ping", null)
			.setHealthCheckUrlsForDeser("http://localhost/ping", null)
            .setVIPAddress("localhost:8080")
			.setVIPAddressDeser("localhost:8080")
			.setSecureVIPAddress("localhost:4443")
			.setSecureVIPAddressDeser("localhost:4443")
			.setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
			.setLeaseInfo(LeaseInfo.Builder.newBuilder()
                    .setDurationInSecs(30)
					.setRenewalIntervalInSecs(30)
					.setEvictionTimestamp(System.currentTimeMillis()+30000)
					.setRenewalTimestamp(System.currentTimeMillis()-1000)
					.setRegistrationTimestamp(System.currentTimeMillis()-2000)
                    .build())
            .add("metadatakey1", "metadatavalue1")
			.setASGName("asg1")
			.setIsCoordinatingDiscoveryServer(false)
			.setLastUpdatedTimestamp(System.currentTimeMillis())
			.setLastDirtyTimestamp(System.currentTimeMillis())
			.setActionType(InstanceInfo.ActionType.ADDED)
			.setNamespace("namespace1")
			.build();

	/**
	 * Simulates Eureka Server own's serialization.
	 * @return
	 */
	@Bean
	public MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter() {
		return new RestTemplateTransportClientFactory()
				.mappingJacksonHttpMessageConverter();
	}

	@ResponseStatus(HttpStatus.OK)
	@PostMapping("/apps/{appName}")
	public void register(@PathVariable String appName,
			@RequestBody InstanceInfo instanceInfo) {
		isTrue(instanceInfo.getPort() != DEFAULT_PORT && instanceInfo.getPort() != 0,
				"Port not received from client");
		isTrue(instanceInfo.getSecurePort() != DEFAULT_SECURE_PORT && instanceInfo.getSecurePort() != 0,
				"Secure Port not received from client");
		// Nothing to do
	}

	@ResponseStatus(HttpStatus.OK)
	@DeleteMapping("/apps/{appName}/{id}")
	public void cancel(@PathVariable String appName, @PathVariable String id) {

	}

	@ResponseStatus(HttpStatus.OK)
	@PutMapping(value = "/apps/{appName}/{id}", params = { "status",
			"lastDirtyTimestamp" })
	public ResponseEntity sendHeartBeat(@PathVariable String appName,
										@PathVariable String id, @RequestParam String status,
										@RequestParam String lastDirtyTimestamp,
										@RequestParam(required = false) String overriddenstatus) {
		if("fourOFour".equals(appName)) {
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<InstanceInfo>(new InstanceInfo(null, null, null, null, null, null, null, null, null,
				null, null, null, null, 0, null, null, null, null, null, null, null, new HashMap<String, String>(), 0l,
				0l, null, null), HttpStatus.OK);
	}

	@ResponseStatus(HttpStatus.OK)
	@PutMapping(value = "/apps/{appName}/{id}/status", params = { "value",
			"lastDirtyTimestamp" })
	public void statusUpdate(@PathVariable String appName, @PathVariable String id,
			@RequestParam String value, @RequestParam String lastDirtyTimestamp) {

	}

	@ResponseStatus(HttpStatus.OK)
	@DeleteMapping(value = "/apps/{appName}/{id}/status", params = "lastDirtyTimestamp")
	public void deleteStatusOverride(@PathVariable String appName,
			@PathVariable String id, @RequestParam String lastDirtyTimestamp) {

	}

	@GetMapping(value = { "/apps", "/apps/delta", "/vips/{address}", "/svips/{address}" })
	public Applications getApplications(@PathVariable(required = false) String address,
			@RequestParam(required = false) String regions) {
		Applications applications = new Applications();
		applications.addApplication(new Application("app1", Collections.singletonList(INFO)));
		return applications;
	}

	@GetMapping(value = "/apps/{appName}")
	public Application getApplication(@PathVariable String appName) {
		return new Application();
	}

	@GetMapping(value = { "/apps/{appName}/{id}", "/instances/{id}" })
	public InstanceInfo getInstance(@PathVariable(required = false) String appName,
			@PathVariable String id) {
		return INFO;
	}

	@Configuration
	@Order(Ordered.HIGHEST_PRECEDENCE)
	protected static class TestSecurityConfiguration extends WebSecurityConfigurerAdapter {


		TestSecurityConfiguration() {
			super(true);
		}

		@Bean
		public UserDetailsService userDetailsService() {
			InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
			manager.createUser(User.withUsername("test").password("{noop}test").roles("USER").build());
			return manager;
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// super.configure(http);
			http.antMatcher("/apps/**")
					.httpBasic();
		}

	}
}
