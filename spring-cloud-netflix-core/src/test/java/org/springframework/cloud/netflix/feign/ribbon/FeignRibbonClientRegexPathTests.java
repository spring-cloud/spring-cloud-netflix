/*
 * Copyright 2016 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Venil Noronha
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = FeignRibbonClientRegexPathTests.Application.class)
@WebIntegrationTest(randomPort = true, value = {
	"spring.application.name=feignclientretrytest",
	"feign.okhttp.enabled=false",
	"feign.httpclient.enabled=false",
	"feign.hystrix.enabled=false"
})
@DirtiesContext
public class FeignRibbonClientRegexPathTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private TestClient testClient;

	@FeignClient("localapp")
	protected interface TestClient {

		@RequestMapping(
			value = "/spring-web/{symbolicName:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{extension:\\.[a-z]+}",
			method = RequestMethod.POST
		)
		ResponseEntity<String> getTest(@PathVariable("symbolicName") String symbolicName,
				@PathVariable("version") String version,
				@PathVariable("extension") String extension);

	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableFeignClients(clients = TestClient.class)
	@RibbonClient(name = "localapp", configuration = LocalRibbonClientConfiguration.class)
	public static class Application {

		@RequestMapping(
			value = "/spring-web/{symbolicName:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{extension:\\.[a-z]+}",
			method = RequestMethod.POST
		)
		public ResponseEntity<String> handle(@PathVariable String symbolicName,
				@PathVariable String version, @PathVariable String extension) {
			return ResponseEntity.ok(symbolicName + "-" + version + extension);
		}

		public static void main(String[] args) throws InterruptedException {
			new SpringApplicationBuilder(Application.class).properties(
				"spring.application.name=feignclientretrytest",
				"management.contextPath=/admin"
			).run(args);
		}

	}

	@Test
	public void testRegexClient() {
		ResponseEntity<String> response = this.testClient.getTest("spring-web", "3.0.5", ".jar");
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("spring-web-3.0.5.jar", response.getBody());
	}

	@Configuration
	public static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
