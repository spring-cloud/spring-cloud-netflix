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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

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
	private static final InstanceInfo INFO = new InstanceInfo(null, null, null, null,
			null, null, null, null, null, null, null, null, null, 0, null, null, null,
			null, null, null, null, 0l, 0l, null, null);

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
	public InstanceInfo sendHeartBeat(@PathVariable String appName,
			@PathVariable String id, @RequestParam String status,
			@RequestParam String lastDirtyTimestamp,
			@RequestParam(required = false) String overriddenstatus) {
		return new InstanceInfo(null, null, null, null, null, null, null, null, null,
				null, null, null, null, 0, null, null, null, null, null, null, null, 0l,
				0l, null, null);
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
		return new Applications();
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
}
