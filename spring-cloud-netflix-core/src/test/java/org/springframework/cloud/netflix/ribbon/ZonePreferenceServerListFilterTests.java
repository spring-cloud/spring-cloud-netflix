/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.ZonePreferenceServerListFilter;
import org.springframework.test.util.ReflectionTestUtils;

import com.netflix.loadbalancer.Server;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class ZonePreferenceServerListFilterTests {

	private Server dsyer = new Server("dsyer", 8080);
	private Server localhost = new Server("localhost", 8080);

	@Before
	public void init() {
		this.dsyer.setZone("dsyer");
		this.localhost.setZone("localhost");
	}

	@Test
	public void noZoneSet() {
		ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
		List<Server> result = filter.getFilteredListOfServers(Arrays
				.asList(this.localhost));
		assertEquals(1, result.size());
	}

	@Test
	public void withZoneSetAndNoMatches() {
		ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
		ReflectionTestUtils.setField(filter, "zone", "dsyer");
		List<Server> result = filter.getFilteredListOfServers(Arrays
				.asList(this.localhost));
		assertEquals(1, result.size());
	}

	@Test
	public void withZoneSetAndMatches() {
		ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
		ReflectionTestUtils.setField(filter, "zone", "dsyer");
		List<Server> result = filter.getFilteredListOfServers(Arrays.asList(this.dsyer,
				this.localhost));
		assertEquals(1, result.size());
	}

}
