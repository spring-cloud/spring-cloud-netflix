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

package org.springframework.cloud.netflix.zuul;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ZuulRefreshRoutesTests.SampleApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"zuul.routes.sampleclient1:/path1/**" })
@DirtiesContext
public class ZuulRefreshRoutesTests {

	@LocalServerPort
	private int port;

	@Autowired
	private ConfigurableEnvironment environment;

	@Autowired
	private ContextRefresher refresher;

	@Test
	public void routesCanBeAddedAndRemovedWithRefresh() {
		TestRestTemplate testRestTemplate = new TestRestTemplate();
		;
		assertThat(testRestTemplate
				.getForEntity("http://localhost:" + this.port + "/path1/sample",
						String.class)
				.getStatusCode()).isEqualTo(HttpStatus.OK);

		assertThat(testRestTemplate
				.getForEntity("http://localhost:" + this.port + "/path2/sample",
						String.class)
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		EnvironmentTestUtils.addEnvironment(this.environment,
				"zuul.routes.sampleclient2=/path2/**");

		refresher.refresh();

		assertThat(testRestTemplate
				.getForEntity("http://localhost:" + this.port + "/path2/sample",
						String.class)
				.getStatusCode()).isEqualTo(HttpStatus.OK);

		removeEnv(environment, "zuul.routes.sampleclient2");

		refresher.refresh();

		assertThat(testRestTemplate
				.getForEntity("http://localhost:" + this.port + "/path2/sample",
						String.class)
				.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

	}

	private void removeEnv(ConfigurableEnvironment environment, String... keys) {
		MutablePropertySources sources = environment.getPropertySources();
		Map<String, Object> map = (Map<String, Object>) sources.get("test").getSource();
		for (String key : keys) {
			map.remove(key);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@EnableZuulProxy
	@RibbonClients({
			@RibbonClient(name = "sampleclient1", configuration = TestRibbonClientConfiguration.class),
			@RibbonClient(name = "sampleclient2", configuration = TestRibbonClientConfiguration.class) })
	static class SampleApp {

		@RequestMapping(value = "/sample", method = RequestMethod.GET)
		public String get() {
			return "body";
		}
	}

	@Configuration
	static class TestRibbonClientConfiguration {

		@LocalServerPort
		private int port;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}
}
