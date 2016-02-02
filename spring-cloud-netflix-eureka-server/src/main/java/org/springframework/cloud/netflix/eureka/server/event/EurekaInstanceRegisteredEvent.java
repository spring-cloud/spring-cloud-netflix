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

import com.netflix.appinfo.InstanceInfo;

import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
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
		return this.instanceInfo;
	}

	public int getLeaseDuration() {
		return this.leaseDuration;
	}

	public boolean isReplication() {
		return this.replication;
	}

	public void setInstanceInfo(InstanceInfo instanceInfo) {
		this.instanceInfo = instanceInfo;
	}

	public void setLeaseDuration(int leaseDuration) {
		this.leaseDuration = leaseDuration;
	}

	public void setReplication(boolean replication) {
		this.replication = replication;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent(instanceInfo="
				+ this.instanceInfo + ", leaseDuration=" + this.leaseDuration
				+ ", replication=" + this.replication + ")";
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof EurekaInstanceRegisteredEvent))
			return false;
		final EurekaInstanceRegisteredEvent other = (EurekaInstanceRegisteredEvent) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$instanceInfo = this.getInstanceInfo();
		final Object other$instanceInfo = other.getInstanceInfo();
		if (this$instanceInfo == null ?
				other$instanceInfo != null :
				!this$instanceInfo.equals(other$instanceInfo))
			return false;
		if (this.getLeaseDuration() != other.getLeaseDuration())
			return false;
		if (this.isReplication() != other.isReplication())
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $instanceInfo = this.getInstanceInfo();
		result = result * PRIME + ($instanceInfo == null ? 0 : $instanceInfo.hashCode());
		result = result * PRIME + this.getLeaseDuration();
		result = result * PRIME + (this.isReplication() ? 79 : 97);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof EurekaInstanceRegisteredEvent;
	}
}
