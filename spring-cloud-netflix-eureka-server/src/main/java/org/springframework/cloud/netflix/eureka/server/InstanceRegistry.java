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

package org.springframework.cloud.netflix.eureka.server;

import java.util.List;

import com.netflix.eureka.lease.Lease;
import org.springframework.beans.BeansException;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ServerCodecs;

import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class InstanceRegistry extends PeerAwareInstanceRegistryImpl
		implements ApplicationContextAware {

	private ApplicationContext ctxt;
	private int defaultOpenForTrafficCount;

	public InstanceRegistry(EurekaServerConfig serverConfig,
			EurekaClientConfig clientConfig, ServerCodecs serverCodecs,
			EurekaClient eurekaClient, int expectedNumberOfRenewsPerMin,
			int defaultOpenForTrafficCount) {
		super(serverConfig, clientConfig, serverCodecs, eurekaClient);

		this.expectedNumberOfRenewsPerMin = expectedNumberOfRenewsPerMin;
		this.defaultOpenForTrafficCount = defaultOpenForTrafficCount;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.ctxt = context;
	}

	/**
	 * If
	 * {@link PeerAwareInstanceRegistryImpl#openForTraffic(ApplicationInfoManager, int)}
	 * is called with a zero * argument, it means that leases are not automatically *
	 * cancelled if the instance * hasn't sent any renewals recently. This happens for a
	 * standalone server. It seems like a bad default, so we set it to the smallest
	 * non-zero value we can, so that any instances that subsequently register can bump up
	 * the threshold.
	 */
	@Override
	public void openForTraffic(ApplicationInfoManager applicationInfoManager, int count) {
		super.openForTraffic(applicationInfoManager,
				count == 0 ? this.defaultOpenForTrafficCount : count);
	}

	@Override
	public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
		logRegistration(info, isReplication, leaseDuration);
		publishEurekaInstanceRegisteredEvent(info, leaseDuration, isReplication);
		super.register(info, leaseDuration, isReplication);
	}

	@Override
	public void register(final InstanceInfo info, final boolean isReplication) {
		final int instanceLeaseDuration = resolveInstanceLeaseDuration(info);
		logRegistration(info, isReplication, instanceLeaseDuration);
		publishEurekaInstanceRegisteredEvent(info, instanceLeaseDuration, isReplication);
		superRegister(info, isReplication);
	}

	@Override
	public boolean cancel(String appName, String serverId, boolean isReplication) {
		logCancelation(appName, serverId, isReplication);
		publishEurekaInstanceCanceledEvent(appName, serverId, isReplication);
		return super.cancel(appName, serverId, isReplication);
	}

	@Override
	public boolean renew(final String appName, final String serverId,
			boolean isReplication) {
		if (log.isDebugEnabled()) {
			log.debug("renew " + appName + " serverId " + serverId + ", isReplication {}"
					+ isReplication);
		}
		List<Application> applications = getSortedApplications();
		for (Application input : applications) {
			if (input.getName().equals(appName)) {
				InstanceInfo instance = null;
				for (InstanceInfo info : input.getInstances()) {
					if (info.getHostName().equals(serverId)) {
						instance = info;
						break;
					}
				}
				this.ctxt.publishEvent(new EurekaInstanceRenewedEvent(this, appName,
						serverId, instance, isReplication));
				break;
			}
		}
		return super.renew(appName, serverId, isReplication);
	}

	@Override
	protected boolean internalCancel(String appName, String id, boolean isReplication) {
		logCancelation(appName, id, isReplication);
		publishEurekaInstanceCanceledEvent(appName, id, isReplication);
		return superInternalCancel(appName, id, isReplication);
	}

	protected boolean superInternalCancel(String appName, String id,
			boolean isReplication) {
		return super.internalCancel(appName, id, isReplication);
	}

	protected void superRegister(InstanceInfo info, boolean isReplication) {
		super.register(info, isReplication);
	}

	private void logRegistration(InstanceInfo info, boolean isReplication,
			int instanceLeaseDuration) {
		if (log.isDebugEnabled()) {
			log.debug("register " + info.getAppName() + ", vip " + info.getVIPAddress()
					+ ", leaseDuration " + instanceLeaseDuration + ", isReplication "
					+ isReplication);
		}
	}

	private void logCancelation(String appName, String serverId, boolean isReplication) {
		if (log.isDebugEnabled()) {
			log.debug("cancel " + appName + " serverId " + serverId + ", isReplication "
					+ isReplication);
		}
	}

	private void publishEurekaInstanceRegisteredEvent(InstanceInfo info,
			int leaseDuration, boolean isReplication) {
		// TODO: what to publish from info (whole object?)
		this.ctxt.publishEvent(new EurekaInstanceRegisteredEvent(this, info,
				leaseDuration, isReplication));
	}

	private void publishEurekaInstanceCanceledEvent(String appName, String serverId,
			boolean isReplication) {
		this.ctxt.publishEvent(
				new EurekaInstanceCanceledEvent(this, appName, serverId, isReplication));
	}

	private int resolveInstanceLeaseDuration(final InstanceInfo info) {
		int leaseDuration = Lease.DEFAULT_DURATION_IN_SECS;
		if (info.getLeaseInfo() != null && info.getLeaseInfo().getDurationInSecs() > 0) {
			leaseDuration = info.getLeaseInfo().getDurationInSecs();
		}
		return leaseDuration;
	}
}
