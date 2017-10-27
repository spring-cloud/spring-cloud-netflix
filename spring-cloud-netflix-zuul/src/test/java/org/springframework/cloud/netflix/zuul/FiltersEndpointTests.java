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
 */

package org.springframework.cloud.netflix.zuul;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.netflix.zuul.ZuulFilter;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests for Filters endpoint
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext
public class FiltersEndpointTests {

	@Autowired
	private FiltersEndpoint endpoint;

	@Test
	public void getFilters() {
		final Map<String, List<Map<String, Object>>> filters = endpoint.invoke();

		boolean foundFilter = false;

		if (filters.containsKey("sample")) {
			for (Map<String, Object> filterInfo : filters.get("sample")) {
				if (TestFilter.class.getName().equals(filterInfo.get("class"))) {
					foundFilter = true;

					// Verify filter's attributes
					assertEquals(0, filterInfo.get("order"));

					break; // the search is over
				}
			}
		}

		assertTrue(foundFilter, "Could not find expected sample filter from filters endpoint");
	}

}

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableZuulProxy
class FiltersEndpointApplication {

	@Bean
	public ZuulFilter sampleFilter() {
		return new TestFilter();
	}

}

class TestFilter extends ZuulFilter {
	@Override
	public String filterType() {
		return "sample";
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		return null;
	}

	@Override
	public int filterOrder() {
		return 0;
	}
}