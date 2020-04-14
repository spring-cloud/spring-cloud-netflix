/*
 * Copyright 2017-2019 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.EurekaHttpResponse.EurekaHttpResponseBuilder;
import com.netflix.discovery.util.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static com.netflix.discovery.shared.transport.EurekaHttpResponse.anEurekaHttpResponse;

/**
 * @author Daniel Lavoie
 * @author Haytham Mohamed
 */
public class WebClientEurekaHttpClient implements EurekaHttpClient {

	protected final Log logger = LogFactory.getLog(getClass());

	private WebClient.Builder webClientBuilder;

	private String serviceUrl;

	public WebClientEurekaHttpClient(WebClient.Builder builder, String serviceUrl) {
		this.webClientBuilder = builder;
		this.serviceUrl = serviceUrl;
		if (!serviceUrl.endsWith("/")) {
			this.serviceUrl = this.serviceUrl + "/";
		}
	}

	@Override
	public EurekaHttpResponse<Void> register(InstanceInfo info) {
		return webClientBuilder.build().post()
				.uri(serviceUrl + "apps/" + info.getAppName(), Void.class)
				.header(HttpHeaders.ACCEPT_ENCODING, "gzip")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.exchange().map(response -> eurekaHttpResponse(response)).block();
	}

	@Override
	public EurekaHttpResponse<Void> cancel(String appName, String id) {
		return webClientBuilder.build().delete()
				.uri(serviceUrl + "apps/" + appName + '/' + id, Void.class).exchange()
				.map(response -> eurekaHttpResponse(response)).block();
	}

	@Override
	public EurekaHttpResponse<InstanceInfo> sendHeartBeat(String appName, String id,
			InstanceInfo info, InstanceStatus overriddenStatus) {
		String urlPath = serviceUrl + "apps/" + appName + '/' + id + "?status="
				+ info.getStatus().toString() + "&lastDirtyTimestamp="
				+ info.getLastDirtyTimestamp().toString() + (overriddenStatus != null
						? "&overriddenstatus=" + overriddenStatus.name() : "");
		return webClientBuilder.build().put().uri(urlPath, InstanceInfo.class).exchange()
				.map(response -> {
					EurekaHttpResponseBuilder<InstanceInfo> builder = anEurekaHttpResponse(
							statusCodeValueOf(response), InstanceInfo.class)
									.headers(headersOf(response));
					InstanceInfo entity = response.toEntity(InstanceInfo.class).block()
							.getBody();

					if (entity != null) {
						builder.entity(entity);
					}
					return builder;
				}).block().build();
	}

	@Override
	public EurekaHttpResponse<Void> statusUpdate(String appName, String id,
			InstanceStatus newStatus, InstanceInfo info) {
		String urlPath = serviceUrl + "apps/" + appName + '/' + id + "/status?value="
				+ newStatus.name() + "&lastDirtyTimestamp="
				+ info.getLastDirtyTimestamp().toString();

		return webClientBuilder.build().put().uri(urlPath, Void.class).exchange()
				.map(response -> eurekaHttpResponse(response)).block();
	}

	@Override
	public EurekaHttpResponse<Void> deleteStatusOverride(String appName, String id,
			InstanceInfo info) {
		String urlPath = serviceUrl + "apps/" + appName + '/' + id
				+ "/status?lastDirtyTimestamp=" + info.getLastDirtyTimestamp().toString();

		return webClientBuilder.build().delete().uri(serviceUrl, Void.class).exchange()
				.map(response -> eurekaHttpResponse(response)).block();
	}

	@Override
	public EurekaHttpResponse<Applications> getApplications(String... regions) {
		return getApplicationsInternal("apps/", regions);
	}

	private EurekaHttpResponse<Applications> getApplicationsInternal(String urlPath,
			String[] regions) {
		String url = serviceUrl + urlPath;

		if (regions != null && regions.length > 0) {
			url = url + (urlPath.contains("?") ? "&" : "?") + "regions="
					+ StringUtil.join(regions);
		}

		ClientResponse response = webClientBuilder.build().get()
				.uri(url, EurekaApplications.class).exchange().block();

		int statusCode = statusCodeValueOf(response);

		EurekaApplications body = response.toEntity(EurekaApplications.class).block()
				.getBody();

		return anEurekaHttpResponse(statusCode,
				statusCode == HttpStatus.OK.value() && body != null ? (Applications) body
						: null).headers(headersOf(response)).build();
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

		ClientResponse response = webClientBuilder.build().get()
				.uri(serviceUrl + "apps/" + appName, Application.class).exchange()
				.block();

		int statusCode = statusCodeValueOf(response);
		Application body = response.toEntity(Application.class).block().getBody();

		Application application = statusCode == HttpStatus.OK.value() && body != null
				? body : null;

		return anEurekaHttpResponse(statusCode, application).headers(headersOf(response))
				.build();
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
		ClientResponse response = webClientBuilder.build().get()
				.uri(serviceUrl + urlPath, InstanceInfo.class).exchange().block();

		int statusCode = statusCodeValueOf(response);
		InstanceInfo body = response.toEntity(InstanceInfo.class).block().getBody();

		return anEurekaHttpResponse(statusCode,
				statusCode == HttpStatus.OK.value() && body != null ? body : null)
						.headers(headersOf(response)).build();
	}

	@Override
	public void shutdown() {
		// Nothing to do
	}

	private static Map<String, String> headersOf(ClientResponse response) {
		ClientResponse.Headers httpHeaders = response.headers();
		if (httpHeaders == null) {
			return Collections.emptyMap();
		}
		HttpHeaders asHeaders = httpHeaders.asHttpHeaders();
		if (asHeaders == null) {
			return Collections.emptyMap();
		}
		Map<String, String> headers = new HashMap<>();
		asHeaders.entrySet().stream().forEach(entry -> entry.getValue().stream()
				.forEach(v -> headers.put(entry.getKey(), v)));
		return headers;
	}

	private int statusCodeValueOf(ClientResponse response) {
		return response.statusCode().value();
	}

	private EurekaHttpResponse<Void> eurekaHttpResponse(ClientResponse response) {
		return anEurekaHttpResponse(statusCodeValueOf(response))
				.headers(headersOf(response)).build();
	}

}
