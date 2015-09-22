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

package org.springframework.cloud.netflix.metrics;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jon Schneider
 */
public class DefaultMetricsTagProvider implements MetricsTagProvider {
	@Override
	public Map<String, String> clientHttpRequestTags(HttpRequest request, ClientHttpResponse response) {
		String urlTemplate = RestTemplateUrlTemplateHolder.getRestTemplateUrlTemplate();
		if (urlTemplate == null)
			urlTemplate = "none";

		String status;
		try {
			status = (response == null) ? "CLIENT_ERROR" : ((Integer) response.getRawStatusCode()).toString();
		} catch (IOException e) {
			status = "IO_ERROR";
		}

		String host = request.getURI().getHost();

		return ImmutableMap.of(
				"method", request.getMethod().name(),
				"uri", sanitizeUrlTemplate(urlTemplate.replaceAll("^https?://[^/]+/", "")),
				"status", status,
				"clientName", host != null ? host : "none");
	}

	@Override
	public Map<String, String> httpRequestTags(HttpServletRequest request, HttpServletResponse response,
			Object handler, String caller) {
		Map<String, String> tags = new HashMap<>();

		tags.put("method", request.getMethod());
		tags.put("status", ((Integer) response.getStatus()).toString());

		String uri = sanitizeUrlTemplate(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
				.toString().substring(1));
		tags.put("uri", uri.isEmpty() ? "root" : uri);

		Object exception = request.getAttribute("exception");
		if (exception != null)
			tags.put("exception", exception.getClass().getSimpleName());

		if (caller != null)
			tags.put("caller", caller);

		return tags;
	}

	/**
	 * As is, the urlTemplate is not suitable for use with Atlas, as all interactions with Atlas take place via query
	 * parameters
	 */
	private String sanitizeUrlTemplate(String urlTemplate) {
		return urlTemplate.replaceAll("/", "_").replaceAll("[{}]", "-");
	}
}
