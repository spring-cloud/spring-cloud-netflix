/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.turbine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TurbineHttpTests.TurbineHttpSampleApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class TurbineHttpTests {

	private static final ClusterInformation foo = new ClusterInformation("foo", "http://foo");
	private static final ClusterInformation bar = new ClusterInformation("bar", "http://bar");

	@Autowired
	TestRestTemplate rest;

	@EnableAutoConfiguration
	@EnableTurbine
	public static class TurbineHttpSampleApplication {

		@Configuration
		static class MyConfig {
			@Bean
			TurbineInformationService myInfoService() {
				return new TurbineInformationService() {
					@Override
					public Collection<ClusterInformation> getClusterInformations(HttpServletRequest request) {
						List<ClusterInformation> clusterInformationList = new ArrayList<ClusterInformation>();
						clusterInformationList.add(foo);
						clusterInformationList.add(bar);
						return clusterInformationList;
					}
				};
			}
		}
	}

	@Test
	public void contextLoads() {
		ClusterInformation[] clusters = rest.getForObject("/clusters", ClusterInformation[].class);
		assertEquals(2, clusters.length);
		assertEquals(foo, clusters[0]);
		assertEquals(bar, clusters[1]);
	}
}
