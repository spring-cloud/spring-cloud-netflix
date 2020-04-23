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

package org.springframework.cloud.netflix.eureka;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;

/**
 * Subclass of {@link DiscoveryClient} that sends a {@link HeartbeatEvent} when
 * {@link CloudEurekaClient#onCacheRefreshed()} is called.
 *
 * @author Spencer Gibb
 */
public class CloudEurekaClient extends DiscoveryClient {

	private static final Log log = LogFactory.getLog(CloudEurekaClient.class);

	private final AtomicLong cacheRefreshedCount = new AtomicLong(0);

	private ApplicationEventPublisher publisher;

	private Field eurekaTransportField;

	private ApplicationInfoManager applicationInfoManager;

	private AtomicReference<EurekaHttpClient> eurekaHttpClient = new AtomicReference<>();

	public CloudEurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig config, ApplicationEventPublisher publisher) {
		this(applicationInfoManager, config, null, publisher);
	}

	public CloudEurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig config, AbstractDiscoveryClientOptionalArgs<?> args,
			ApplicationEventPublisher publisher) {
		super(applicationInfoManager, config, args);
		this.applicationInfoManager = applicationInfoManager;
		this.publisher = publisher;
		this.eurekaTransportField = ReflectionUtils.findField(DiscoveryClient.class,
				"eurekaTransport");
		ReflectionUtils.makeAccessible(this.eurekaTransportField);
	}

	public ApplicationInfoManager getApplicationInfoManager() {
		return applicationInfoManager;
	}

	public void cancelOverrideStatus(InstanceInfo info) {
		getEurekaHttpClient().deleteStatusOverride(info.getAppName(), info.getId(), info);
	}

	public InstanceInfo getInstanceInfo(String appname, String instanceId) {
		EurekaHttpResponse<InstanceInfo> response = getEurekaHttpClient()
				.getInstance(appname, instanceId);
		HttpStatus httpStatus = HttpStatus.valueOf(response.getStatusCode());
		if (httpStatus.is2xxSuccessful() && response.getEntity() != null) {
			return response.getEntity();
		}
		return null;
	}

	EurekaHttpClient getEurekaHttpClient() {
		if (this.eurekaHttpClient.get() == null) {
			try {
				Object eurekaTransport = this.eurekaTransportField.get(this);
				Field registrationClientField = ReflectionUtils
						.findField(eurekaTransport.getClass(), "registrationClient");
				ReflectionUtils.makeAccessible(registrationClientField);
				this.eurekaHttpClient.compareAndSet(null,
						(EurekaHttpClient) registrationClientField.get(eurekaTransport));
			}
			catch (IllegalAccessException e) {
				log.error("error getting EurekaHttpClient", e);
			}
		}
		return this.eurekaHttpClient.get();
	}

	public void setStatus(InstanceStatus newStatus, InstanceInfo info) {
		getEurekaHttpClient().statusUpdate(info.getAppName(), info.getId(), newStatus,
				info);
	}

	@Override
	protected void onCacheRefreshed() {
		super.onCacheRefreshed();

		if (this.cacheRefreshedCount != null) { // might be called during construction and
			// will be null
			long newCount = this.cacheRefreshedCount.incrementAndGet();
			log.trace("onCacheRefreshed called with count: " + newCount);
			this.publisher.publishEvent(new HeartbeatEvent(this, newCount));
		}
	}

}
