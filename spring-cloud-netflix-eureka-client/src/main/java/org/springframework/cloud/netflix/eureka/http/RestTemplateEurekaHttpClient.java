/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.EurekaHttpResponse.EurekaHttpResponseBuilder;
import com.netflix.discovery.util.StringUtil;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;

/**
 * @author Daniel Lavoie
 * @author Václav Plic
 * @deprecated {@link RestTemplate}-based implementation to be removed in favour of
 * {@link RestClient}-based implementation.
 */
@Deprecated(forRemoval = true)
public class RestTemplateEurekaHttpClient implements EurekaHttpClient {

	private final RestTemplate restTemplate;

	private String serviceUrl;

	public RestTemplateEurekaHttpClient(RestTemplate restTemplate, String serviceUrl) {
		this.restTemplate = restTemplate;
		this.serviceUrl = serviceUrl;
		if (!serviceUrl.endsWith("/")) {
			this.serviceUrl = this.serviceUrl + "/";
		}
	}

	public String getServiceUrl() {
		return this.serviceUrl;
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	@Override
	public EurekaHttpResponse<Void> register(InstanceInfo info) {
		URI uri = UriComponentsBuilder.fromHttpUrl(serviceUrl)
			.path("apps/{appName}")
			.buildAndExpand(info.getAppName())
			.toUri();

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip");
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(info, headers),
				Void.class);

		return anEurekaHttpResponse(response.getStatusCode().value()).headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<Void> cancel(String appName, String id) {
		URI uri = UriComponentsBuilder.fromHttpUrl(serviceUrl)
			.path("apps/{appName}/{id}")
			.buildAndExpand(appName, id)
			.toUri();

		ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, Void.class);

		return anEurekaHttpResponse(response.getStatusCode().value()).headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id, InstanceInfo info,
			InstanceStatus overriddenStatus) {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(serviceUrl)
			.path("apps/{appName}/{id}")
			.queryParam("status", info.getStatus().toString())
			.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString());

		if (overriddenStatus != null) {
			uriBuilder = uriBuilder.queryParam("overriddenstatus", overriddenStatus.name());
		}

		URI uri = uriBuilder.buildAndExpand(appName, id).toUri();

		ResponseEntity<InstanceInfo> response = restTemplate.exchange(uri, HttpMethod.PUT, null, InstanceInfo.class);

		EurekaHttpResponseBuilder<InstanceInfo> eurekaResponseBuilder = anEurekaHttpResponse(
				response.getStatusCode().value(), InstanceInfo.class)
			.headers(headersOf(response));

		if (response.hasBody()) {
			eurekaResponseBuilder.entity(response.getBody());
		}

		return eurekaResponseBuilder.build();
	}

	@Override
	public EurekaHttpResponse<Void> statusUpdate(String appName, String id, InstanceStatus newStatus,
			InstanceInfo info) {
		URI uri = UriComponentsBuilder.fromHttpUrl(serviceUrl)
			.path("apps/{appName}/{id}/status")
			.queryParam("value", newStatus.name())
			.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
			.buildAndExpand(appName, id)
			.toUri();

		ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, null, Void.class);

		return anEurekaHttpResponse(response.getStatusCode().value()).headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<Void> deleteStatusOverride(String appName, String id, InstanceInfo info) {
		URI uri = UriComponentsBuilder.fromHttpUrl(serviceUrl)
			.path("apps/{appName}/{id}/status")
			.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
			.buildAndExpand(appName, id)
			.toUri();

		ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.DELETE, null, Void.class);

		return anEurekaHttpResponse(response.getStatusCode().value()).headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<Applications> getApplications(String... regions) {
		return getApplicationsInternal("apps/", regions);
	}

	private EurekaHttpResponse<Applications> getApplicationsInternal(String urlPath, String[] regions) {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(serviceUrl).path(urlPath);

		if (regions != null && regions.length > 0) {
			uriBuilder = uriBuilder.queryParam("regions", StringUtil.join(regions));
		}

		URI uri = uriBuilder.build().toUri();

		ResponseEntity<EurekaApplications> response = restTemplate.exchange(uri, HttpMethod.GET, null,
				EurekaApplications.class);

		return anEurekaHttpResponse(response.getStatusCode().value(),
				response.getStatusCode().value() == HttpStatus.OK.value() && response.hasBody()
						? (Applications) response.getBody() : null)
			.headers(headersOf(response))
			.build();
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
	public EurekaHttpResponse<Applications> getSecureVip(String secureVipAddress, String... regions) {
		return getApplicationsInternal("svips/" + secureVipAddress, regions);
	}

	@Override
	public EurekaHttpResponse<Application> getApplication(String appName) {
		URI uri = UriComponentsBuilder.fromHttpUrl(serviceUrl).path("apps/{appName}").buildAndExpand(appName).toUri();

		ResponseEntity<Application> response = restTemplate.exchange(uri, HttpMethod.GET, null, Application.class);

		Application application = response.getStatusCode().value() == HttpStatus.OK.value() && response.hasBody()
				? response.getBody() : null;

		return anEurekaHttpResponse(response.getStatusCode().value(), application).headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> getInstance(String appName, String id) {
		return getInstanceInternal("apps", appName, id);
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> getInstance(String id) {
		return getInstanceInternal("instances", id);
	}

	private EurekaHttpResponse<InstanceInfo> getInstanceInternal(String... pathSegments) {
		URI uri = UriComponentsBuilder.fromHttpUrl(serviceUrl).pathSegment(pathSegments).build().toUri();

		ResponseEntity<InstanceInfo> response = restTemplate.exchange(uri, HttpMethod.GET, null, InstanceInfo.class);

		return anEurekaHttpResponse(response.getStatusCode().value(),
				response.getStatusCode().value() == HttpStatus.OK.value() && response.hasBody() ? response.getBody()
						: null)
			.headers(headersOf(response))
			.build();
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
