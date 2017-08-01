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

import java.util.Objects;

/**
 * @author Spencer Gibb
 * @author Gregor Zurowski
 */
@SuppressWarnings("serial")
public class EurekaInstanceCanceledEvent extends ApplicationEvent {

	private String appName;

	private String serverId;

	private boolean replication;

	public EurekaInstanceCanceledEvent(Object source, String appName, String serverId,
			boolean replication) {
		super(source);
		this.appName = appName;
		this.serverId = serverId;
		this.replication = replication;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
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
		EurekaInstanceCanceledEvent that = (EurekaInstanceCanceledEvent) o;
		return Objects.equals(appName, that.appName) &&
				Objects.equals(serverId, that.serverId) &&
				replication == replication;
	}

	@Override
	public int hashCode() {
		return Objects.hash(appName, serverId, replication);
	}

	@Override
	public String toString() {
		return new StringBuilder("EurekaInstanceCanceledEvent{")
				.append("appName='").append(appName).append("', ")
				.append("serverId='").append(serverId).append("', ")
				.append("replication=").append(replication).append("}")
				.toString();
	}

}
