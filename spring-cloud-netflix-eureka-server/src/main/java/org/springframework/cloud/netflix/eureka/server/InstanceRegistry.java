/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.lease.Lease;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.EurekaServerHttpClientFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 * @author Wonchul Heo
 */
public class InstanceRegistry extends PeerAwareInstanceRegistryImpl implements ApplicationContextAware {

	private static final Log log = LogFactory.getLog(InstanceRegistry.class);

	private ApplicationContext ctxt;

	private final int defaultOpenForTrafficCount;

	public InstanceRegistry(EurekaServerConfig serverConfig, EurekaClientConfig clientConfig, ServerCodecs serverCodecs,
			EurekaClient eurekaClient, EurekaServerHttpClientFactory eurekaServerHttpClientFactory,
			int expectedNumberOfClientsSendingRenews, int defaultOpenForTrafficCount) {
		super(serverConfig, clientConfig, serverCodecs, eurekaClient, eurekaServerHttpClientFactory);

		this.expectedNumberOfClientsSendingRenews = expectedNumberOfClientsSendingRenews;
		this.defaultOpenForTrafficCount = defaultOpenForTrafficCount;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.ctxt = context;
	}

	/**
	 * If
	 * {@link PeerAwareInstanceRegistryImpl#openForTraffic(ApplicationInfoManager, int)}
	 * is called with a zero argument, it means that leases are not automatically
	 * cancelled if the instance hasn't sent any renewals recently. This happens for a
	 * standalone server. It seems like a bad default, so we set it to the smallest
	 * non-zero value we can, so that any instances that subsequently register can bump up
	 * the threshold.
	 */
	@Override
	public void openForTraffic(ApplicationInfoManager applicationInfoManager, int count) {
		super.openForTraffic(applicationInfoManager, count == 0 ? this.defaultOpenForTrafficCount : count);
	}

	@Override
	public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
		super.register(info, leaseDuration, isReplication);
		handleRegistration(info, leaseDuration, isReplication);
	}

	@Override
	public void register(final InstanceInfo info, final boolean isReplication) {
		super.register(info, isReplication);
		handleRegistration(info, resolveInstanceLeaseDuration(info), isReplication);
	}

	@Override
	public boolean cancel(String appName, String serverId, boolean isReplication) {
		final boolean cancelled = super.cancel(appName, serverId, isReplication);
		if (cancelled) {
			handleCancelation(appName, serverId, isReplication);
		}
		return cancelled;
	}

	@Override
	public boolean renew(final String appName, final String serverId, boolean isReplication) {
		final boolean renewed = super.renew(appName, serverId, isReplication);
		if (renewed) {
			handleRenewal(appName, serverId, isReplication);
		}
		return renewed;
	}

	@Override
	protected boolean internalCancel(String appName, String id, boolean isReplication) {
		final boolean cancelled = super.internalCancel(appName, id, isReplication);
		if (cancelled) {
			handleCancelation(appName, id, isReplication);
		}
		return cancelled;
	}

	private void handleCancelation(String appName, String id, boolean isReplication) {
		log("cancelled " + appName + ", serverId " + id + ", isReplication " + isReplication);
		publishEvent(new EurekaInstanceCanceledEvent(this, appName, id, isReplication));
	}

	private void handleRegistration(InstanceInfo info, int leaseDuration, boolean isReplication) {
		log("registered " + info.getAppName() + ", vip " + info.getVIPAddress() + ", leaseDuration " + leaseDuration
				+ ", isReplication " + isReplication);
		publishEvent(new EurekaInstanceRegisteredEvent(this, info, leaseDuration, isReplication));
	}

	private void handleRenewal(final String appName, final String serverId, boolean isReplication) {
		log("renewed " + appName + ", serverId " + serverId + ", isReplication " + isReplication);
		final Application application = getApplication(appName);
		if (application != null) {
			final InstanceInfo instanceInfo = application.getByInstanceId(serverId);
			if (instanceInfo != null) {
				publishEvent(new EurekaInstanceRenewedEvent(this, appName, serverId, instanceInfo, isReplication));
			}
		}
	}

	private void log(String message) {
		if (log.isDebugEnabled()) {
			log.debug(message);
		}
	}

	private void publishEvent(ApplicationEvent applicationEvent) {
		this.ctxt.publishEvent(applicationEvent);
	}

	private int resolveInstanceLeaseDuration(final InstanceInfo info) {
		int leaseDuration = Lease.DEFAULT_DURATION_IN_SECS;
		if (info.getLeaseInfo() != null && info.getLeaseInfo().getDurationInSecs() > 0) {
			leaseDuration = info.getLeaseInfo().getDurationInSecs();
		}
		return leaseDuration;
	}

}
