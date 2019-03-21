/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.Mockito.when;

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
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get("uid")).isNotNull();
		assertThat(result.get("uid")).hasSize(1);
		Assert.assertThat(result.get("uid"), hasItem(hasProperty("body", equalTo("65"))));
		Assert.assertThat(result.get("uid"),
				hasItem(hasProperty("headers", notNullValue())));
	}

	@Test
	public void methodExtractShouldReturnNotDuplicatedValuesFromRequestWhenEncoded()
			throws Exception {
		// when
		when(request.getMultiFileMap()).thenReturn(new LinkedMultiValueMap<>());
		when(request.getQueryString()).thenReturn("uid=hello%20world");

		Map<String, String[]> expectedParameterMap = new HashMap<String, String[]>() {
			{
				put("uid", new String[] { "hello world" });
			}
		};
		when(request.getParameterMap()).thenReturn(expectedParameterMap);

		// action
		MultiValueMap<String, Object> result = RequestContentDataExtractor
				.extract(request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(0);
	}

	@Test
	public void findQueryParamsGroupedByNameShouldReturnCorrectResult() {
		// when
		when(request.getQueryString()).thenReturn("uid=12&uid=34");

		// action
		Map<String, List<String>> result = RequestContentDataExtractor
				.findQueryParamsGroupedByName(request);

		// then
		assertThat(result).containsEntry("uid", asList("12", "34"));
		assertThat(result.size()).isEqualTo(1);
	}

	@Test
	public void findQueryParamsGroupedByNameShouldReturnEmptyMapWhenQueryIsEmpty() {
		// when
		when(request.getQueryString()).thenReturn("");

		// action
		Map<String, List<String>> result = RequestContentDataExtractor
				.findQueryParamsGroupedByName(request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(0);
	}

	@Test
	public void findQueryParamsGroupedByNameShouldReturnEmptyMapWhenQueryIsNull() {
		// when
		when(request.getQueryString()).thenReturn(null);

		// action
		Map<String, List<String>> result = RequestContentDataExtractor
				.findQueryParamsGroupedByName(request);

		// then
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(0);
	}

	@Test
	public void findQueryParamsGroupedByNameShouldReturnEmptyMapWhenQueryValueIsNull()
			throws IOException {
		// when
		Map<String, String[]> paramMap = new HashMap<>();
		paramMap.put("uid", new String[] { "foo", "bar" });
		when(request.getQueryString()).thenReturn("uid");
		when(request.getParameterMap()).thenReturn(paramMap);
		when(request.getMultiFileMap()).thenReturn(new LinkedMultiValueMap<>());

		// action
		Map<String, List<Object>> result = RequestContentDataExtractor.extract(request);

		// then
		List<Object> uidResult = new LinkedList();
		uidResult.add(new HttpEntity<>("foo"));
		uidResult.add(new HttpEntity<>("bar"));
		assertThat(result).isNotNull();
		assertThat(result).containsEntry("uid", uidResult);
		assertThat(result.size()).isEqualTo(1);
	}

}
