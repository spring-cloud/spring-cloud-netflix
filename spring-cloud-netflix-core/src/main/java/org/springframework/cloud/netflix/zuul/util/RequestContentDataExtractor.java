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

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class RequestContentDataExtractor {
	public static MultiValueMap<String, Object> extract(HttpServletRequest request) throws IOException {
		return (request instanceof MultipartHttpServletRequest) ?
				extractFromMultipartRequest((MultipartHttpServletRequest) request) :
				extractFromRequest(request);
	}

	private static MultiValueMap<String, Object> extractFromRequest(HttpServletRequest request) throws IOException {
		MultiValueMap<String, Object> builder = new LinkedMultiValueMap<>();
		Set<String>	queryParams = findQueryParams(request);

		for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			String key = entry.getKey();

			if (!queryParams.contains(key) && entry.getValue() != null) {
				for (String value : entry.getValue()) {
					builder.add(key, value);
				}
			}
		}

		return builder;
	}

	private static MultiValueMap<String, Object> extractFromMultipartRequest(MultipartHttpServletRequest request)
			throws IOException {
		MultiValueMap<String, Object> builder = new LinkedMultiValueMap<>();
		Set<String>	queryParams = findQueryParams(request);

		for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			String key = entry.getKey();

			if (!queryParams.contains(key)) {
				for (String value : entry.getValue()) {
					HttpHeaders headers = new HttpHeaders();
					String	  type	= request.getMultipartContentType(key);

					if (type != null) {
						headers.setContentType(MediaType.valueOf(type));
					}

					builder.add(key, new HttpEntity<>(value, headers));
				}
			}
		}

		for (Entry<String, List<MultipartFile>> parts : request.getMultiFileMap().entrySet()) {
			for (MultipartFile file : parts.getValue()) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentDispositionFormData(file.getName(), file.getOriginalFilename());
				if (file.getContentType() != null) {
					headers.setContentType(MediaType.valueOf(file.getContentType()));
				}

				HttpEntity entity = new HttpEntity<>(new InputStreamResource(file.getInputStream()), headers);
				builder.add(parts.getKey(), entity);
			}
		}

		return builder;
	}

	private static Set<String> findQueryParams(HttpServletRequest request) {
		Set<String> result = new HashSet<>();
		String query  = request.getQueryString();

		if (query != null) {
			for (String value : StringUtils.tokenizeToStringArray(query, "&")) {
				if (value.contains("=")) {
					value = value.substring(0, value.indexOf("="));
				}
				result.add(value);
			}
		}

		return result;
	}
}
