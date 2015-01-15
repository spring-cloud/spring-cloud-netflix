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

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.context.ApplicationEvent;

import com.netflix.appinfo.InstanceInfo;

/**
 * @author Spencer Gibb
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("serial")
public class EurekaInstanceRenewedEvent extends ApplicationEvent {

	private String appName;

	private String serverId;

	private InstanceInfo instanceInfo;

	private boolean replication;

	public EurekaInstanceRenewedEvent(Object source, String appName, String serverId,
			InstanceInfo instanceInfo, boolean replication) {
		super(source);
		this.appName = appName;
		this.serverId = serverId;
		this.instanceInfo = instanceInfo;
		this.replication = replication;
	}

}
