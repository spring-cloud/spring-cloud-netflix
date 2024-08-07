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

import java.util.Map;
import java.util.Optional;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.EurekaHttpResponse.EurekaHttpResponseBuilder;
import com.netflix.discovery.util.StringUtil;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;

/**
 * @author Daniel Lavoie
 * @author Haytham Mohamed
 * @author Václav Plic
 */
public class WebClientEurekaHttpClient implements EurekaHttpClient {

	private WebClient webClient;

	public WebClientEurekaHttpClient(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public EurekaHttpResponse<Void> register(InstanceInfo info) {
		return webClient.post()
			.uri(uriBuilder -> uriBuilder.path("apps/{appName}").build(info.getAppName()))
			.body(BodyInserters.fromValue(info))
			.header(HttpHeaders.ACCEPT_ENCODING, "gzip")
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toBodilessEntity()
			.map(this::eurekaHttpResponse)
			.block();
	}

	@Override
	public EurekaHttpResponse<Void> cancel(String appName, String id) {
		return webClient.delete()
			.uri(uriBuilder -> uriBuilder.path("apps/{appName}/{id}").build(appName, id))
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toBodilessEntity()
			.map(this::eurekaHttpResponse)
			.block();
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id, InstanceInfo info,
			InstanceStatus overriddenStatus) {

		ResponseEntity<InstanceInfo> response = webClient.put()
			.uri(uriBuilder -> uriBuilder.path("apps/{appName}/{id}")
				.queryParam("status", info.getStatus().toString())
				.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
				.build(appName, id))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toEntity(InstanceInfo.class)
			.block();

		EurekaHttpResponseBuilder<InstanceInfo> builder = anEurekaHttpResponse(statusCodeValueOf(response),
				InstanceInfo.class)
			.headers(headersOf(response));

		InstanceInfo entity = response.getBody();

		if (entity != null) {
			builder.entity(entity);
		}

		return builder.build();

	}

	@Override
	public EurekaHttpResponse<Void> statusUpdate(String appName, String id, InstanceStatus newStatus,
			InstanceInfo info) {
		return webClient.put()
			.uri(uriBuilder -> uriBuilder.path("apps/{appName}/{id}/status")
				.queryParam("value", newStatus.name())
				.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
				.build(appName, id))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toBodilessEntity()
			.map(this::eurekaHttpResponse)
			.block();
	}

	@Override
	public EurekaHttpResponse<Void> deleteStatusOverride(String appName, String id, InstanceInfo info) {
		return webClient.delete()
			.uri(uriBuilder -> uriBuilder.path("apps/{appName}/{id}/status")
				.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
				.build(appName, id))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toBodilessEntity()
			.map(this::eurekaHttpResponse)
			.block();
	}

	@Override
	public EurekaHttpResponse<Applications> getApplications(String... regions) {
		return getApplicationsInternal("apps/", regions);
	}

	private EurekaHttpResponse<Applications> getApplicationsInternal(String urlPath, String[] regions) {
		Optional<String> regionsParam = (regions != null && regions.length > 0) ? Optional.of(StringUtil.join(regions))
				: Optional.empty();

		ResponseEntity<Applications> response = webClient.get()
			.uri(uriBuilder -> uriBuilder.path(urlPath).queryParamIfPresent("regions", regionsParam).build())
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toEntity(Applications.class)
			.block();

		int statusCode = statusCodeValueOf(response);

		Applications body = response.getBody();

		return anEurekaHttpResponse(statusCode, statusCode == HttpStatus.OK.value() && body != null ? body : null)
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

		ResponseEntity<Application> response = webClient.get()
			.uri(uriBuilder -> uriBuilder.path("apps/{appName}").build(appName))
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toEntity(Application.class)
			.block();

		int statusCode = statusCodeValueOf(response);
		Application body = response.getBody();

		Application application = statusCode == HttpStatus.OK.value() && body != null ? body : null;

		return anEurekaHttpResponse(statusCode, application).headers(headersOf(response)).build();
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
		ResponseEntity<InstanceInfo> response = webClient.get()
			.uri(uriBuilder -> uriBuilder.pathSegment(pathSegments).build())
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.onStatus(HttpStatusCode::isError, this::ignoreError)
			.toEntity(InstanceInfo.class)
			.block();

		int statusCode = statusCodeValueOf(response);
		InstanceInfo body = response.getBody();

		return anEurekaHttpResponse(statusCode, statusCode == HttpStatus.OK.value() && body != null ? body : null)
			.headers(headersOf(response))
			.build();
	}

	@Override
	public void shutdown() {
		// Nothing to do
	}

	public WebClient getWebClient() {
		return this.webClient;
	}

	private Mono<? extends Throwable> ignoreError(ClientResponse response) {
		return Mono.empty();
	}

	private static Map<String, String> headersOf(ResponseEntity<?> response) {
		return response.getHeaders().toSingleValueMap();
	}

	private int statusCodeValueOf(ResponseEntity<?> response) {
		return response.getStatusCode().value();
	}

	private EurekaHttpResponse<Void> eurekaHttpResponse(ResponseEntity<?> response) {
		return anEurekaHttpResponse(statusCodeValueOf(response)).headers(headersOf(response)).build();
	}

}
