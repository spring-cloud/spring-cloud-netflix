/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Spencer Gibb
 */
public class ProxyRequestHelperTests {

	@Mock
	private TraceRepository traceRepository;

	@Before
	public void init() {
		initMocks(this);
	}

	@Test
	public void buildZuulRequestHeadersWork() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("singleName", "singleValue");
		request.addHeader("multiName", "multiValue1");
		request.addHeader("multiName", "multiValue2");

		ProxyRequestHelper helper = new ProxyRequestHelper();
		helper.setTraces(traceRepository);

		MultiValueMap<String, String> headers = helper.buildZuulRequestHeaders(request);
		List<String> singleName = headers.get("singleName");
		assertThat(singleName, is(notNullValue()));
		assertThat(singleName.size(), is(1));

		List<String> multiName = headers.get("multiName");
		assertThat(multiName, is(notNullValue()));
		assertThat(multiName.size(), is(2));

		List<String> missingName = headers.get("missingName");
		assertThat(missingName, is(nullValue()));

	}
}
