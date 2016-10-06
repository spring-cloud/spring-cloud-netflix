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

package org.springframework.cloud.netflix.eureka;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.EurekaClient;

/**
 * Endpoint to display and set the eureka instance status
 *
 * @author Spencer Gibb
 */
@ManagedResource(description = "Can be used to display and set the eureka instance status")
public class EurekaEndpoint implements MvcEndpoint, ApplicationContextAware {

	private ApplicationInfoManager infoManager;
	private EurekaHealthCheckHandler eurekaHealthCheckHandler;
	private CloudEurekaClient eurekaClient;
	private ApplicationContext context;

	@Autowired
	public EurekaEndpoint(ApplicationInfoManager infoManager, EurekaHealthCheckHandler eurekaHealthCheckHandler) {
		this.infoManager = infoManager;
		this.eurekaHealthCheckHandler = eurekaHealthCheckHandler;
	}

	@RequestMapping(path = "instance-status", method = RequestMethod.DELETE)
	@ResponseBody
	@ManagedOperation
	public void deleteStatus() {
		getEurekaClient().cancelOverrideStatus();
	}

	@RequestMapping(path = "instance-status", method = RequestMethod.POST)
	@ResponseBody
	@ManagedOperation
	public void setStatus(@RequestBody Status status) {
		Assert.notNull(status, "status may not by null");
		getEurekaClient().setStatus(status.getStatus());
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	CloudEurekaClient getEurekaClient() {
		if (this.eurekaClient == null) {
			EurekaClient client = this.context.getBean(EurekaClient.class);
			try {
				this.eurekaClient = getTargetObject(client, CloudEurekaClient.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return this.eurekaClient;
	}

	@SuppressWarnings({"unchecked"})
	protected <T> T getTargetObject(Object proxy, Class<T> targetClass) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return (T) proxy; // expected to be cglib proxy then, which is simply a specialized class
		}
	}

	@RequestMapping(path = "instance-status", method = RequestMethod.GET)
	@ResponseBody
	@ManagedAttribute
	public Status getStatus() {
		return new Status(this.infoManager.getInfo().getStatus(), this.infoManager.getInfo().getOverriddenStatus());
	}

	@Override
	public String getPath() {
		return "/eureka";
	}

	@Override
	public boolean isSensitive() {
		return true;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

	static class Status {
		private InstanceStatus status;
		private InstanceStatus overriddenStatus;

		public Status(InstanceStatus status, InstanceStatus overriddenStatus) {
			this.status = status;
			this.overriddenStatus = overriddenStatus;
		}

		private Status() {
		}

		public InstanceStatus getStatus() {
			return status;
		}

		public void setStatus(InstanceStatus status) {
			this.status = status;
		}

		public InstanceStatus getOverriddenStatus() {
			return overriddenStatus;
		}

		public void setOverriddenStatus(InstanceStatus overriddenStatus) {
			this.overriddenStatus = overriddenStatus;
		}
	}
}
