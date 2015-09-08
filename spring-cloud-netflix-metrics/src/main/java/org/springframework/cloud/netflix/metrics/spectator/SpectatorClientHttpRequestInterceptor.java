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

package org.springframework.cloud.netflix.metrics.spectator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.netflix.spectator.api.Registry;

/**
 * Intercepts RestTemplate requests and records metrics about execution time and results.
 *
 * @author Jon Schneider
 */
public class SpectatorClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
	@Autowired
	Registry registry;

	@Value("${netflix.spectator.restClient.metricName:restclient}")
	String metricName;

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		String urlTemplate = RestTemplateUrlTemplateHolder.getRestTemplateUrlTemplate();
		if (urlTemplate == null)
			urlTemplate = "none";

		long startTime = registry.clock().wallTime();
		String status = "CLIENT_ERROR";
		try {
			ClientHttpResponse response = execution.execute(request, body);
			status = ((Integer) response.getRawStatusCode()).toString();
			return response;
		} finally {
			String host = request.getURI().getHost();

			registry.timer(metricName, "method", request.getMethod().name(), "uri",
					urlTemplate.replaceAll("^https?://[^/]+/", "").replaceAll("/", "_").replaceAll("[{}]", "-"),
					"status", status, "clientName", host != null ? host : "none").record(
					registry.clock().wallTime() - startTime, TimeUnit.MILLISECONDS);
		}
	}
}
