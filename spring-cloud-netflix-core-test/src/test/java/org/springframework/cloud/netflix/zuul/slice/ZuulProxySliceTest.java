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
import org.springframework.context.ApplicationContext;
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
// tag::sample_zuul_slice_test[]
@RunWith(SpringRunner.class)
@ZuulProxyTest(properties = {
	// @formatter:off
	"zuul.routes.sample1.path=/sample1/**",
	"sample1.ribbon.listOfServers=http://localhost:${wiremock.server.port}",

	"zuul.routes.sample2.path=/sample2/**", 
	"zuul.routes.sample2.strip-prefix=false",
	"sample2.ribbon.listOfServers=localhost:${wiremock.server.port}"
	// @formatter:on
})
@DirtiesContext
public class ZuulProxySliceTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Before
	public void setup() {
		stubFor(WireMock.get(urlEqualTo("/test"))
				.willReturn(aResponse().withStatus(200).withBody("sample1")));
		
		stubFor(WireMock.get(urlEqualTo("/sample2/test"))
				.willReturn(aResponse().withStatus(200).withBody("sample2")));

	}

	@Test
	public void testZuulProxySlice() throws Exception {
		mockMvc.perform(get("/sample1/test")).andExpect(status().is2xxSuccessful())
				.andExpect(header().string("SOME_HEADER", "SOME_VALUE"))
				.andExpect(content().string("sample1"));

		mockMvc.perform(get("/sample2/test")).andExpect(status().is2xxSuccessful())
				.andExpect(header().string("SOME_HEADER", "SOME_VALUE"))
				.andExpect(content().string("sample2"));
		
		// Scan should not find beans of type other than ZuulFilter
		assertThat(applicationContext.getBeansOfType(SampleZuulFilter.class))
				.isNotEmpty();
		assertThat(applicationContext.getBeansOfType(SomeServiceBean.class)).isEmpty();
	}

}
// end::sample_zuul_slice_test[]
