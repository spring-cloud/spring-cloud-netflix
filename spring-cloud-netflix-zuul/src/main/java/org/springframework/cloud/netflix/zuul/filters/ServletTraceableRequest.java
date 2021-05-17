/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.trace.http.TraceableRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

/**
 * @author Spencer Gibb
 */	
public class ServletTraceableRequest implements TraceableRequest {

		private HttpServletRequest request;

		ServletTraceableRequest(HttpServletRequest request) {
			this.request = request;
		}

		@Override
		public String getMethod() {
			return request.getMethod();
		}

		@Override
		public URI getUri() {
			String queryString = this.request.getQueryString();
			if (!StringUtils.hasText(queryString)) {
				return URI.create(this.request.getRequestURL().toString());
			}
			try {
				StringBuffer urlBuffer = appendQueryString(queryString);
				return new URI(urlBuffer.toString());
			}
			catch (URISyntaxException ex) {
				String encoded = UriUtils.encode(queryString, StandardCharsets.UTF_8);
				StringBuffer urlBuffer = appendQueryString(encoded);
				return URI.create(urlBuffer.toString());
			}
		}

		private StringBuffer appendQueryString(String queryString) {
			StringBuffer urlBuffer = this.request.getRequestURL();
			urlBuffer.append("?");
			urlBuffer.append(queryString);
			return urlBuffer;
		}

		@Override
		public Map<String, List<String>> getHeaders() {
			return extractHeaders();
		}

		@Override
		public String getRemoteAddress() {
			return request.getRemoteAddr();
		}

		private Map<String, List<String>> extractHeaders() {
			Map<String, List<String>> headers = new LinkedHashMap<>();
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				headers.put(name, toList(request.getHeaders(name)));
			}
			return headers;
		}

		private List<String> toList(Enumeration<String> enumeration) {
			List<String> list = new ArrayList<>();
			while (enumeration.hasMoreElements()) {
				list.add(enumeration.nextElement());
			}
			return list;
		}
	}
