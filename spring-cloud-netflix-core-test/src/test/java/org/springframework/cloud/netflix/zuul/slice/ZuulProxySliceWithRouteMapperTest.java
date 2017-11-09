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

package org.springframework.cloud.netflix.zuul.slice;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.netflix.zuul.filters.discovery.PatternServiceRouteMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@ZuulProxyTest(properties = {
	// @formatter:off
	"sample3-v1.ribbon.listOfServers=localhost:${wiremock.server.port}",
	"spring.cloud.discovery.client.simple.instances.sample3-v1[0].host=localhost"
	// @formatter:on
})
@DirtiesContext
public class ZuulProxySliceWithRouteMapperTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Before
	public void setup() {
		stubFor(WireMock.get(urlEqualTo("/test"))
				.willReturn(aResponse().withStatus(200).withBody("sample3")));
	}

	@Test
	public void testZuulProxySlice() throws Exception {
		mockMvc.perform(get("/v1/sample3/test")).andExpect(status().is2xxSuccessful())
				.andExpect(content().string("sample3"));
	}
	
	@TestConfiguration
	static class AdditionalConfig {
		@Bean
		public PatternServiceRouteMapper serviceRouteMapper() {
			return new PatternServiceRouteMapper(
				"(?<name>^.+)-(?<version>v.+$)",
				"${version}/${name}");
		}
	}

}
