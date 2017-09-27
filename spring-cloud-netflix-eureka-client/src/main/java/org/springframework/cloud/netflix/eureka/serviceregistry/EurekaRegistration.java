/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.eureka.serviceregistry;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.CloudEurekaInstanceConfig;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Spencer Gibb
 */
public class EurekaRegistration implements Registration, Closeable {
	private static final Log log = LogFactory.getLog(EurekaRegistration.class);

	private final EurekaClient eurekaClient;
	private final AtomicReference<CloudEurekaClient> cloudEurekaClient = new AtomicReference<>();
	private final CloudEurekaInstanceConfig instanceConfig;
	private final ApplicationInfoManager applicationInfoManager;
	private HealthCheckHandler healthCheckHandler;

	private EurekaRegistration(CloudEurekaInstanceConfig instanceConfig, EurekaClient eurekaClient, ApplicationInfoManager applicationInfoManager, HealthCheckHandler healthCheckHandler) {
		this.eurekaClient = eurekaClient;
		this.instanceConfig = instanceConfig;
		this.applicationInfoManager = applicationInfoManager;
		this.healthCheckHandler = healthCheckHandler;
	}

	public static Builder builder(CloudEurekaInstanceConfig instanceConfig) {
		return new Builder(instanceConfig);
	}

	public static class Builder {
		private final CloudEurekaInstanceConfig instanceConfig;
		private ApplicationInfoManager applicationInfoManager;
		private EurekaClient eurekaClient;
		private HealthCheckHandler healthCheckHandler;

		private EurekaClientConfig clientConfig;
		private ApplicationEventPublisher publisher;

		Builder(CloudEurekaInstanceConfig instanceConfig) {
			this.instanceConfig = instanceConfig;
		}

		public Builder with(ApplicationInfoManager applicationInfoManager) {
			this.applicationInfoManager = applicationInfoManager;
			return this;
		}

		public Builder with(EurekaClient eurekaClient) {
			this.eurekaClient = eurekaClient;
			return this;
		}

		public Builder with(HealthCheckHandler healthCheckHandler) {
			this.healthCheckHandler = healthCheckHandler;
			return this;
		}

		public Builder with(EurekaClientConfig clientConfig, ApplicationEventPublisher publisher) {
			this.clientConfig = clientConfig;
			this.publisher = publisher;
			return this;
		}

		public EurekaRegistration build() {
			Assert.notNull(instanceConfig, "instanceConfig may not be null");

			if (this.applicationInfoManager == null) {
				InstanceInfo instanceInfo = new InstanceInfoFactory().create(this.instanceConfig);
				this.applicationInfoManager = new ApplicationInfoManager(this.instanceConfig, instanceInfo);
			}
			if (this.eurekaClient == null) {
				Assert.notNull(this.clientConfig, "if eurekaClient is null, EurekaClientConfig may not be null");
				Assert.notNull(this.publisher, "if eurekaClient is null, ApplicationEventPublisher may not be null");

				this.eurekaClient = new CloudEurekaClient(this.applicationInfoManager, this.clientConfig, this.publisher);
			}
			return new EurekaRegistration(instanceConfig, eurekaClient, applicationInfoManager, healthCheckHandler);
		}

	}

	@Override
	public String getServiceId() {
		return this.instanceConfig.getAppname();
	}

	@Override
	public String getHost() {
		return this.instanceConfig.getHostName(false);
	}

	@Override
	public int getPort() {
		if (this.instanceConfig.getSecurePortEnabled()) {
			return this.instanceConfig.getSecurePort();
		}
		return this.instanceConfig.getNonSecurePort();
	}

	@Override
	public boolean isSecure() {
		return this.instanceConfig.getSecurePortEnabled();
	}

	@Override
	public URI getUri() {
		return DefaultServiceInstance.getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		return this.instanceConfig.getMetadataMap();
	}

	public CloudEurekaClient getEurekaClient() {
		if (this.cloudEurekaClient.get() == null) {
			try {
				this.cloudEurekaClient.compareAndSet(null, getTargetObject(eurekaClient, CloudEurekaClient.class));
			} catch (Exception e) {
				log.error("error getting CloudEurekaClient", e);
			}
		}
		return this.cloudEurekaClient.get();
	}

	@SuppressWarnings({"unchecked"})
	protected <T> T getTargetObject(Object proxy, Class<T> targetClass) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return (T) proxy; // expected to be cglib proxy then, which is simply a specialized class
		}
	}

	public CloudEurekaInstanceConfig getInstanceConfig() {
		return instanceConfig;
	}

	public ApplicationInfoManager getApplicationInfoManager() {
		return applicationInfoManager;
	}

	public HealthCheckHandler getHealthCheckHandler() {
		return healthCheckHandler;
	}

	public void setHealthCheckHandler(HealthCheckHandler healthCheckHandler) {
		this.healthCheckHandler = healthCheckHandler;
	}

	public void setNonSecurePort(int port) {
		this.instanceConfig.setNonSecurePort(port);
	}

	public int getNonSecurePort() {
		return this.instanceConfig.getNonSecurePort();
	}

	public void setSecurePort(int port) {
		this.instanceConfig.setSecurePort(port);
	}

	public int getSecurePort() {
		return this.instanceConfig.getSecurePort();
	}

	@Override
	public void close() throws IOException {
		this.eurekaClient.shutdown();
	}
}
