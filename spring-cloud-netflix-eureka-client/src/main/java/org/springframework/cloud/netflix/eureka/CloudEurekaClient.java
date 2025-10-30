/*
 * Copyright 2013-present the original author or authors.
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
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.Lifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;

/**
 * Subclass of {@link DiscoveryClient} that sends a {@link HeartbeatEvent} when
 * {@link CloudEurekaClient#onCacheRefreshed()} is called.
 *
 * @author Spencer Gibb
 */
public class CloudEurekaClient extends DiscoveryClient implements Lifecycle {

	private static final Log log = LogFactory.getLog(CloudEurekaClient.class);

	private final AtomicLong cacheRefreshedCount = new AtomicLong(0);

	private final ApplicationEventPublisher publisher;

	private final Field eurekaTransportField;

	private final ApplicationInfoManager applicationInfoManager;

	private final AtomicReference<EurekaHttpClient> eurekaHttpClient = new AtomicReference<>();

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = true;

	public CloudEurekaClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config,
			TransportClientFactories transportClientFactories, ApplicationEventPublisher publisher) {
		this(applicationInfoManager, config, transportClientFactories, null, publisher);
	}

	public CloudEurekaClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config,
			TransportClientFactories transportClientFactories, AbstractDiscoveryClientOptionalArgs<?> args,
			ApplicationEventPublisher publisher) {
		super(applicationInfoManager, config, transportClientFactories, args);
		this.applicationInfoManager = applicationInfoManager;
		this.publisher = publisher;
		eurekaTransportField = ReflectionUtils.findField(DiscoveryClient.class, "eurekaTransport");
		ReflectionUtils.makeAccessible(eurekaTransportField);
	}

	public ApplicationInfoManager getApplicationInfoManager() {
		return applicationInfoManager;
	}

	public void cancelOverrideStatus(InstanceInfo info) {
		getEurekaHttpClient().deleteStatusOverride(info.getAppName(), info.getId(), info);
	}

	public InstanceInfo getInstanceInfo(String appname, String instanceId) {
		EurekaHttpResponse<InstanceInfo> response = getEurekaHttpClient().getInstance(appname, instanceId);
		HttpStatus httpStatus = HttpStatus.valueOf(response.getStatusCode());
		if (httpStatus.is2xxSuccessful() && response.getEntity() != null) {
			return response.getEntity();
		}
		return null;
	}

	EurekaHttpClient getEurekaHttpClient() {
		if (eurekaHttpClient.get() == null) {
			try {
				Object eurekaTransport = eurekaTransportField.get(this);
				Field registrationClientField = ReflectionUtils.findField(eurekaTransport.getClass(),
						"registrationClient");
				ReflectionUtils.makeAccessible(registrationClientField);
				eurekaHttpClient.compareAndSet(null, (EurekaHttpClient) registrationClientField.get(eurekaTransport));
			}
			catch (IllegalAccessException e) {
				log.error("error getting EurekaHttpClient", e);
			}
		}
		return eurekaHttpClient.get();
	}

	public void setStatus(InstanceStatus newStatus, InstanceInfo info) {
		getEurekaHttpClient().statusUpdate(info.getAppName(), info.getId(), newStatus, info);
	}

	@Override
	protected void onCacheRefreshed() {
		super.onCacheRefreshed();

		if (cacheRefreshedCount != null) { // might be called during construction and
			// will be null
			long newCount = cacheRefreshedCount.incrementAndGet();
			log.trace("onCacheRefreshed called with count: " + newCount);
			publisher.publishEvent(new HeartbeatEvent(this, newCount));
		}
	}

	@Override
	public void start() {
		synchronized (lifecycleMonitor) {
			if (!isRunning()) {
				running = true;
			}
		}
	}

	@Override
	public void stop() {
		synchronized (lifecycleMonitor) {
			if (isRunning()) {
				applicationInfoManager.refreshLeaseInfoIfRequired();
				shutdown();
				running = false;
			}
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

}
