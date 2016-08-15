/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import lombok.extern.apachecommons.CommonsLog;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationContext;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import org.springframework.util.ReflectionUtils;

/**
 * Subclass of {@link DiscoveryClient} that sends a {@link HeartbeatEvent} when
 * {@link CloudEurekaClient#onCacheRefreshed()} is called.
 * @author Spencer Gibb
 */
@CommonsLog
public class CloudEurekaClient extends DiscoveryClient {

	private final AtomicLong cacheRefreshedCount = new AtomicLong(0);

	private ApplicationContext context;
	private Field eurekaTransportField;
	private ApplicationInfoManager applicationInfoManager;
	private EurekaHttpClient eurekaHttpClient;

	public CloudEurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig config, ApplicationContext context) {
		this(applicationInfoManager, config, null, context);
	}

	public CloudEurekaClient(ApplicationInfoManager applicationInfoManager,
							 EurekaClientConfig config,
							 DiscoveryClientOptionalArgs args,
							 ApplicationContext context) {
		super(applicationInfoManager, config, args);
		this.applicationInfoManager = applicationInfoManager;
		this.context = context;
		this.eurekaTransportField = ReflectionUtils.findField(DiscoveryClient.class, "eurekaTransport");
		ReflectionUtils.makeAccessible(this.eurekaTransportField);
	}

	public void cancelOverrideStatus() {
		InstanceInfo info = this.applicationInfoManager.getInfo();
		getEurekaHttpClient().deleteStatusOverride(info.getAppName(), info.getId(), info);
	}

	EurekaHttpClient getEurekaHttpClient() {
		if (eurekaHttpClient == null) {
			try {
				Object eurekaTransport = this.eurekaTransportField.get(this);
				Field registrationClientField = ReflectionUtils.findField(eurekaTransport.getClass(), "registrationClient");
				ReflectionUtils.makeAccessible(registrationClientField);
				eurekaHttpClient = (EurekaHttpClient) registrationClientField.get(eurekaTransport);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return eurekaHttpClient;
	}

	public void setStatus(InstanceStatus newStatus) {
		InstanceInfo info = this.applicationInfoManager.getInfo();
		getEurekaHttpClient().statusUpdate(info.getAppName(), info.getId(), newStatus, info);
	}

	@Override
	protected void onCacheRefreshed() {
		if (this.cacheRefreshedCount != null) { //might be called during construction and will be null
			long newCount = this.cacheRefreshedCount.incrementAndGet();
			log.trace("onCacheRefreshed called with count: " + newCount);
			this.context.publishEvent(new HeartbeatEvent(this, newCount));
		}
	}
}
