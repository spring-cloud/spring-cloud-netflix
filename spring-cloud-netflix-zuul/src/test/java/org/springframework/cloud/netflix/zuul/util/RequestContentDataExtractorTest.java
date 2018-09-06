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

package org.springframework.cloud.netflix.zuul.util;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * Created by Dmitrii_Priporov on 01.07.18.
 */
@RunWith(MockitoJUnitRunner.class)
public class RequestContentDataExtractorTest {

	@Mock
	private MultipartHttpServletRequest request;

	@Test
	public void methodExtractShouldReturnNotDuplicatedValuesFromRequest()
			throws Exception {
		// when
		when(request.getMultiFileMap()).thenReturn(new LinkedMultiValueMap<>());
		when(request.getQueryString()).thenReturn("uid=12&uid=34");

		Map<String, String[]> expectedParameterMap = new HashMap<String, String[]>() {
			{
				put("uid", new String[] { "65" });
			}
		};
		when(request.getParameterMap()).thenReturn(expectedParameterMap);

		// action
		MultiValueMap<String, Object> result = RequestContentDataExtractor
				.extract(request);

		// then
		assertThat(result, notNullValue());
		assertThat(result.size(), equalTo(1));
		assertThat(result.get("uid"), notNullValue());
		assertThat(result.get("uid"), hasSize(1));
		assertThat(result.get("uid"), hasItem(hasProperty("body", equalTo("65"))));
		assertThat(result.get("uid"), hasItem(hasProperty("headers", notNullValue())));
	}

	@Test
	public void findQueryParamsGroupedByNameShouldReturnCorrectResult() {
		// when
		when(request.getQueryString()).thenReturn("uid=12&uid=34");

		// action
		Map<String, List<String>> result = RequestContentDataExtractor
				.findQueryParamsGroupedByName(request);

		// then
		assertThat(result, hasEntry("uid", asList("12", "34")));
		assertThat(result.size(), equalTo(1));
	}

	@Test
	public void findQueryParamsGroupedByNameShouldReturnEmptyMapWhenQueryIsEmpty() {
		// when
		when(request.getQueryString()).thenReturn("");

		// action
		Map<String, List<String>> result = RequestContentDataExtractor
				.findQueryParamsGroupedByName(request);

		// then
		assertThat(result, notNullValue());
		assertThat(result.size(), equalTo(0));
	}

	@Test
	public void findQueryParamsGroupedByNameShouldReturnEmptyMapWhenQueryIsNull() {
		// when
		when(request.getQueryString()).thenReturn(null);

		// action
		Map<String, List<String>> result = RequestContentDataExtractor
				.findQueryParamsGroupedByName(request);

		// then
		assertThat(result, notNullValue());
		assertThat(result.size(), equalTo(0));
	}

}