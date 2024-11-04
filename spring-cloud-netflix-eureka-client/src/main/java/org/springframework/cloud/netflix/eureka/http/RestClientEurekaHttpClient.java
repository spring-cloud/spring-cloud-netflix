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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.EurekaHttpResponse.EurekaHttpResponseBuilder;
import com.netflix.discovery.util.StringUtil;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;

/**
 * {@link RestClient} implementation of {@link EurekaHttpClient}.
 *
 * @author Wonchul Heo
 * @since 4.2.0
 */
public class RestClientEurekaHttpClient implements EurekaHttpClient {

	private final RestClient restClient;

	public RestClientEurekaHttpClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public EurekaHttpResponse<Void> register(InstanceInfo info) {
		final ResponseEntity<Void> response = restClient.post()
			.uri(builder -> builder.pathSegment("apps", info.getAppName()).build())
			.body(info)
			.header(HttpHeaders.ACCEPT_ENCODING, "gzip")
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.toBodilessEntity();
		return eurekaHttpResponse(response);
	}

	@Override
	public EurekaHttpResponse<Void> cancel(String appName, String id) {
		final ResponseEntity<Void> response = restClient.delete()
			.uri(builder -> builder.pathSegment("apps", appName, id).build())
			.retrieve()
			.toBodilessEntity();
		return eurekaHttpResponse(response);
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id, InstanceInfo info,
			InstanceStatus overriddenStatus) {
		final Function<UriBuilder, URI> uriFunction = builder -> builder.pathSegment("apps", appName, id)
			.queryParam("status", info.getStatus().toString())
			.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
			.queryParamIfPresent("overriddenstatus", Optional.ofNullable(overriddenStatus).map(InstanceStatus::name))
			.build();

		final ResponseEntity<InstanceInfo> response = restClient.put()
			.uri(uriFunction)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.toEntity(InstanceInfo.class);

		final EurekaHttpResponseBuilder<InstanceInfo> builder = anEurekaHttpResponse(statusCodeValueOf(response),
				InstanceInfo.class)
			.headers(headersOf(response));

		final InstanceInfo entity = response.getBody();
		if (entity != null) {
			builder.entity(entity);
		}

		return builder.build();
	}

	@Override
	public EurekaHttpResponse<Void> statusUpdate(String appName, String id, InstanceStatus newStatus,
			InstanceInfo info) {
		final Function<UriBuilder, URI> uriFunction = builder -> builder.pathSegment("apps", appName, id, "status")
			.queryParam("value", newStatus.name())
			.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
			.build();

		final ResponseEntity<Void> response = restClient.put()
			.uri(uriFunction)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.toBodilessEntity();
		return eurekaHttpResponse(response);
	}

	@Override
	public EurekaHttpResponse<Void> deleteStatusOverride(String appName, String id, InstanceInfo info) {
		final Function<UriBuilder, URI> uriFunction = builder -> builder.pathSegment("apps", appName, id, "status")
			.queryParam("lastDirtyTimestamp", info.getLastDirtyTimestamp().toString())
			.build();

		final ResponseEntity<Void> response = restClient.delete()
			.uri(uriFunction)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.toBodilessEntity();
		return eurekaHttpResponse(response);
	}

	@Override
	public EurekaHttpResponse<Applications> getApplications(String... regions) {
		return getApplicationsInternal("/apps/", regions);
	}

	@Override
	public EurekaHttpResponse<Applications> getDelta(String... regions) {
		return getApplicationsInternal("/apps/delta", regions);
	}

	@Override
	public EurekaHttpResponse<Applications> getVip(String vipAddress, String... regions) {
		return getApplicationsInternal("/vips/" + vipAddress, regions);
	}

	@Override
	public EurekaHttpResponse<Applications> getSecureVip(String secureVipAddress, String... regions) {
		return getApplicationsInternal("/svips/" + secureVipAddress, regions);
	}

	@Override
	public EurekaHttpResponse<Application> getApplication(String appName) {

		final ResponseEntity<Application> response = restClient.get()
			.uri("/apps/" + appName)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.toEntity(Application.class);

		final int statusCode = statusCodeValueOf(response);
		final Application body = response.getBody();

		final Application application = statusCode == HttpStatus.OK.value() && body != null ? body : null;

		return anEurekaHttpResponse(statusCode, application).headers(headersOf(response)).build();
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> getInstance(String appName, String id) {
		return getInstanceInternal("/apps/" + appName + '/' + id);
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> getInstance(String id) {
		return getInstanceInternal("/instances/" + id);
	}

	@Override
	public void shutdown() {
	}

	public RestClient getRestClient() {
		return restClient;
	}

	private EurekaHttpResponse<Applications> getApplicationsInternal(String urlPath, String[] regions) {
		final Function<UriBuilder, URI> uriFunction = builder -> builder
			.queryParamIfPresent("regions",
					Optional.ofNullable(regions).filter(it -> it.length > 0).map(StringUtil::join))
			.build();

		final ResponseEntity<Applications> response = restClient.get()
			.uri(urlPath, uriFunction)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.toEntity(Applications.class);

		final int statusCode = statusCodeValueOf(response);
		final Applications body = response.getBody();

		return anEurekaHttpResponse(statusCode, statusCode == HttpStatus.OK.value() && body != null ? body : null)
			.headers(headersOf(response))
			.build();
	}

	private EurekaHttpResponse<InstanceInfo> getInstanceInternal(String urlPath) {
		final ResponseEntity<InstanceInfo> response = restClient.get()
			.uri(urlPath)
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.retrieve()
			.toEntity(InstanceInfo.class);

		final int statusCode = statusCodeValueOf(response);
		final InstanceInfo body = response.getBody();

		return anEurekaHttpResponse(statusCode, statusCode == HttpStatus.OK.value() && body != null ? body : null)
			.headers(headersOf(response))
			.build();
	}

	private static Map<String, String> headersOf(ResponseEntity<?> response) {
		return response.getHeaders().toSingleValueMap();
	}

	private static int statusCodeValueOf(ResponseEntity<?> response) {
		return response.getStatusCode().value();
	}

	private static EurekaHttpResponse<Void> eurekaHttpResponse(ResponseEntity<?> response) {
		return anEurekaHttpResponse(statusCodeValueOf(response)).headers(headersOf(response)).build();
	}

}
