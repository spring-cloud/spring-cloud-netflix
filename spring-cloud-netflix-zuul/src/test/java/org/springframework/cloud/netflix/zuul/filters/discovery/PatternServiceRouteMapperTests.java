/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.discovery;

import com.netflix.zuul.context.RequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stéphane Leroy
 */
public class PatternServiceRouteMapperTests {

	/**
	 * Service pattern that follow convention {domain}-{name}-{version}. The name is
	 * optional
	 */
	public static final String SERVICE_PATTERN = "(?<domain>^\\w+)(-(?<name>\\w+)-|-)(?<version>v\\d+$)";

	public static final String ROUTE_PATTERN = "${version}/${domain}/${name}";

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void test_return_mapped_route_if_serviceid_matches() {
		PatternServiceRouteMapper toTest = new PatternServiceRouteMapper(SERVICE_PATTERN,
				ROUTE_PATTERN);

		assertThat(toTest.apply("rest-service-v1")).as("service version convention")
				.isEqualTo("v1/rest/service");
	}

	@Test
	public void test_return_serviceid_if_no_matches() {
		PatternServiceRouteMapper toTest = new PatternServiceRouteMapper(SERVICE_PATTERN,
				ROUTE_PATTERN);

		// No version here
		assertThat(toTest.apply("rest-service")).as("No matches for this service id")
				.isEqualTo("rest-service");
	}

	@Test
	public void test_route_should_be_cleaned_before_returned() {
		// Messy patterns
		PatternServiceRouteMapper toTest = new PatternServiceRouteMapper(
				SERVICE_PATTERN + "(?<nevermatch>.)?",
				"/${version}/${nevermatch}/${domain}/${name}/");
		assertThat(toTest.apply("domain-service-v1")).as("No matches for this service id")
				.isEqualTo("v1/domain/service");
		assertThat(toTest.apply("domain-v1")).as("No matches for this service id")
				.isEqualTo("v1/domain");
	}

}
