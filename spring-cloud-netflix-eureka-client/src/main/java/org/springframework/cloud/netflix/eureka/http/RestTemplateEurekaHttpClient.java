/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.EurekaHttpResponse.EurekaHttpResponseBuilder;
import com.netflix.discovery.util.StringUtil;

/**
 * @author Daniel Lavoie
 */
public class RestTemplateEurekaHttpClient implements EurekaHttpClient {

	protected final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;
	private String serviceUrl;

	public RestTemplateEurekaHttpClient(RestTemplate restTemplate, String serviceUrl) {
		this.restTemplate = restTemplate;
		this.serviceUrl = serviceUrl;
	}

	@Override
	public EurekaHttpResponse<Void> register(InstanceInfo info) {
		String urlPath = serviceUrl + "apps/" + info.getAppName();

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip");
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.POST,
				new HttpEntity<InstanceInfo>(info, headers), Void.class);

		return anEurekaHttpResponse(response.getStatusCodeValue())
				.headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<Void> cancel(String appName, String id) {
		String urlPath = serviceUrl + "apps/" + appName + '/' + id;

		ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.DELETE,
				null, Void.class);

		return anEurekaHttpResponse(response.getStatusCodeValue())
				.headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id,
			InstanceInfo info, InstanceStatus overriddenStatus) {
		String urlPath = serviceUrl + "apps/" + appName + '/' + id + "?status="
				+ info.getStatus().toString() + "&lastDirtyTimestamp="
				+ info.getLastDirtyTimestamp().toString() + (overriddenStatus != null
						? "&overriddenstatus=" + overriddenStatus.name() : "");

		ResponseEntity<InstanceInfo> response = restTemplate.exchange(urlPath,
				HttpMethod.PUT, null, InstanceInfo.class);

		EurekaHttpResponseBuilder<InstanceInfo> eurekaResponseBuilder = anEurekaHttpResponse(
				response.getStatusCodeValue(), InstanceInfo.class)
						.headers(headersOf(response));

		if (response.hasBody())
			eurekaResponseBuilder.entity(response.getBody());

		return eurekaResponseBuilder.build();
	}

	@Override
	public EurekaHttpResponse<Void> statusUpdate(String appName, String id,
			InstanceStatus newStatus, InstanceInfo info) {
		String urlPath = serviceUrl + "apps/" + appName + '/' + id + "?status="
				+ newStatus.name() + "&lastDirtyTimestamp="
				+ info.getLastDirtyTimestamp().toString();

		ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.PUT,
				null, Void.class);

		return anEurekaHttpResponse(response.getStatusCodeValue())
				.headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<Void> deleteStatusOverride(String appName, String id,
			InstanceInfo info) {
		String urlPath = serviceUrl + "apps/" + appName + '/' + id
				+ "/status?lastDirtyTimestamp=" + info.getLastDirtyTimestamp().toString();

		ResponseEntity<Void> response = restTemplate.exchange(urlPath, HttpMethod.DELETE,
				null, Void.class);

		return anEurekaHttpResponse(response.getStatusCodeValue())
				.headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<Applications> getApplications(String... regions) {
		return getApplicationsInternal("apps/", regions);
	}

	private EurekaHttpResponse<Applications> getApplicationsInternal(String urlPath,
			String[] regions) {
		String url = serviceUrl + urlPath;

		if (regions != null && regions.length > 0)
			urlPath = (urlPath.contains("?") ? "&" : "?") + "regions="
					+ StringUtil.join(regions);

		ResponseEntity<EurekaApplications> response = restTemplate.exchange(url,
				HttpMethod.GET, null, EurekaApplications.class);

		return anEurekaHttpResponse(response.getStatusCodeValue(),
				response.getStatusCode().value() == HttpStatus.OK.value()
						&& response.hasBody() ? (Applications) response.getBody() : null)
								.headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<Applications> getDelta(String... regions) {
		return getApplicationsInternal("apps/delta", regions);
	}

	@Override
	public EurekaHttpResponse<Applications> getVip(String vipAddress, String... regions) {
		return getApplicationsInternal("vips/" + vipAddress, regions);
	}

	@Override
	public EurekaHttpResponse<Applications> getSecureVip(String secureVipAddress,
			String... regions) {
		return getApplicationsInternal("svips/" + secureVipAddress, regions);
	}

	@Override
	public EurekaHttpResponse<Application> getApplication(String appName) {
		String urlPath = serviceUrl + "apps/" + appName;

		ResponseEntity<Application> response = restTemplate.exchange(urlPath,
				HttpMethod.GET, null, Application.class);

		Application application = response.getStatusCodeValue() == HttpStatus.OK.value()
				&& response.hasBody() ? response.getBody() : null;

		return anEurekaHttpResponse(response.getStatusCodeValue(), application)
				.headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> getInstance(String appName, String id) {
		return getInstanceInternal("apps/" + appName + '/' + id);
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> getInstance(String id) {
		return getInstanceInternal("instances/" + id);
	}

	private EurekaHttpResponse<InstanceInfo> getInstanceInternal(String urlPath) {
		urlPath = serviceUrl + urlPath;

		ResponseEntity<InstanceInfo> response = restTemplate.exchange(urlPath,
				HttpMethod.GET, null, InstanceInfo.class);

		return anEurekaHttpResponse(response.getStatusCodeValue(),
				response.getStatusCodeValue() == HttpStatus.OK.value()
						&& response.hasBody() ? response.getBody() : null)
								.headers(headersOf(response)).build();
	}

	@Override
	public void shutdown() {
		// Nothing to do
	}

	private static Map<String, String> headersOf(ResponseEntity<?> response) {
		HttpHeaders httpHeaders = response.getHeaders();
		if (httpHeaders == null || httpHeaders.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> headers = new HashMap<>();
		for (Entry<String, List<String>> entry : httpHeaders.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				headers.put(entry.getKey(), entry.getValue().get(0));
			}
		}
		return headers;
	}
}
