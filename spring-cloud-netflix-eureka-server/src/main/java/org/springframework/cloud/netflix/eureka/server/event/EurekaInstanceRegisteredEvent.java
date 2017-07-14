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

package org.springframework.cloud.netflix.eureka.server.event;

import org.springframework.context.ApplicationEvent;

import com.netflix.appinfo.InstanceInfo;

import java.util.Objects;

/**
 * @author Spencer Gibb
 * @author Gregor Zurowski
 */
@SuppressWarnings("serial")
public class EurekaInstanceRegisteredEvent extends ApplicationEvent {

	private InstanceInfo instanceInfo;

	private int leaseDuration;

	private boolean replication;

	public EurekaInstanceRegisteredEvent(Object source, InstanceInfo instanceInfo,
			int leaseDuration, boolean replication) {
		super(source);
		this.instanceInfo = instanceInfo;
		this.leaseDuration = leaseDuration;
		this.replication = replication;
	}

	public InstanceInfo getInstanceInfo() {
		return instanceInfo;
	}

	public void setInstanceInfo(InstanceInfo instanceInfo) {
		this.instanceInfo = instanceInfo;
	}

	public int getLeaseDuration() {
		return leaseDuration;
	}

	public void setLeaseDuration(int leaseDuration) {
		this.leaseDuration = leaseDuration;
	}

	public boolean isReplication() {
		return replication;
	}

	public void setReplication(boolean replication) {
		this.replication = replication;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EurekaInstanceRegisteredEvent that = (EurekaInstanceRegisteredEvent) o;
		return Objects.equals(instanceInfo, that.instanceInfo) &&
				leaseDuration == leaseDuration &&
				replication == replication;
	}

	@Override
	public int hashCode() {
		return Objects.hash(instanceInfo, leaseDuration, replication);
	}

	@Override
	public String toString() {
		return new StringBuilder("EurekaInstanceRegisteredEvent{")
				.append("instanceInfo=").append(instanceInfo).append(", ")
				.append("leaseDuration=").append(leaseDuration).append(", ")
				.append("replication=").append(replication).append("}")
				.toString();
	}
}
