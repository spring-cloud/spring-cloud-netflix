/*
 * Copyright 2013-2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics.servo;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.cloud.netflix.metrics.DefaultMetricsTagProvider;
import org.springframework.cloud.netflix.metrics.MetricsTagProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class DefaultMetricsTagProviderTests {
	MetricsTagProvider provider = new DefaultMetricsTagProvider();
	HttpServletResponse response = new MockHttpServletResponse();
	
	@Test
	public void encodeSlashes() {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/test/some");
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, "/test/some");
		assertUriTagIsEqualTo(request, "test_some");
	}
	
	@Test
	public void encodePathVariables() {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/test/some/1");
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, "/test/some/{id}");
		assertUriTagIsEqualTo(request, "test_some_-id-");
	}
	
	@Test
	public void encodeRegexBasedUri() {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/test/some/regex/a{}");
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, "/test/some/regex/{id:.+{}}");
		assertUriTagIsEqualTo(request, "test_some_regex_-id-");
	}
	
	private void assertUriTagIsEqualTo(HttpServletRequest request, String expectedTag) {
		assertThat(provider.httpRequestTags(request, response, null, null).get("uri"), is(equalTo(expectedTag)));
	}
}
