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

package org.springframework.cloud.netflix.eureka.serviceregistry;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.netflix.appinfo.InstanceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UNKNOWN;

/**
 * @author Spencer Gibb
 * @author Robert Bleyl
 */
public class EurekaServiceRegistry implements ServiceRegistry<EurekaRegistration> {

	private static final Log log = LogFactory.getLog(EurekaServiceRegistry.class);

	private EurekaInstanceConfigBean eurekaInstanceConfigBean;

	public EurekaServiceRegistry() {

	}

	public EurekaServiceRegistry(EurekaInstanceConfigBean eurekaInstanceConfigBean) {
		this.eurekaInstanceConfigBean = eurekaInstanceConfigBean;
	}

	@Override
	public void register(EurekaRegistration reg) {
		if (eurekaInstanceConfigBean != null && eurekaInstanceConfigBean.isAsyncClientInitialization()) {
			if (log.isDebugEnabled()) {
				log.debug("Initializing client asynchronously...");
			}

			ExecutorService executorService = Executors.newSingleThreadExecutor();
			executorService.submit(() -> {
				maybeInitializeClient(reg);
				if (log.isDebugEnabled()) {
					log.debug("Asynchronous client initialization done.");
				}
			});
		}
		else {
			maybeInitializeClient(reg);
		}

		if (log.isInfoEnabled()) {
			log.info("Registering application " + reg.getApplicationInfoManager().getInfo().getAppName()
					+ " with eureka with status " + reg.getInstanceConfig().getInitialStatus());
		}

		reg.getApplicationInfoManager().setInstanceStatus(reg.getInstanceConfig().getInitialStatus());

		reg.getHealthCheckHandler()
			.ifAvailable(healthCheckHandler -> reg.getEurekaClient().registerHealthCheck(healthCheckHandler));
	}

	private void maybeInitializeClient(EurekaRegistration reg) {
		// force initialization of possibly scoped proxies
		reg.getApplicationInfoManager().getInfo();
		reg.getEurekaClient().getApplications();
	}

	@Override
	public void deregister(EurekaRegistration reg) {
		if (reg.getApplicationInfoManager().getInfo() != null) {

			if (log.isInfoEnabled()) {
				log.info("Unregistering application " + reg.getApplicationInfoManager().getInfo().getAppName()
						+ " with eureka with status DOWN");
			}

			reg.getApplicationInfoManager().setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);

			// shutdown of eureka client should happen with EurekaRegistration.close()
			// auto registration will create a bean which will be properly disposed
			// manual registrations will need to call close()
		}
	}

	@Override
	public void setStatus(EurekaRegistration registration, String status) {
		InstanceInfo info = registration.getApplicationInfoManager().getInfo();

		// TODO: howto deal with delete properly?
		if ("CANCEL_OVERRIDE".equalsIgnoreCase(status)) {
			registration.getEurekaClient().cancelOverrideStatus(info);
			return;
		}

		// TODO: howto deal with status types across discovery systems?
		InstanceInfo.InstanceStatus newStatus = InstanceInfo.InstanceStatus.toEnum(status);
		registration.getEurekaClient().setStatus(newStatus, info);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getStatus(EurekaRegistration registration) {
		String appname = registration.getApplicationInfoManager().getInfo().getAppName();
		String instanceId = registration.getApplicationInfoManager().getInfo().getId();
		InstanceInfo info = registration.getEurekaClient().getInstanceInfo(appname, instanceId);

		HashMap<String, Object> status = new HashMap<>();
		if (info != null) {
			status.put("status", info.getStatus().toString());
			status.put("overriddenStatus", info.getOverriddenStatus().toString());
		}
		else {
			status.put("status", UNKNOWN.toString());
		}

		return status;
	}

	public void close() {
	}

}
