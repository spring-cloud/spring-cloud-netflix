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
	boolean replication;

	public EurekaInstanceRenewedEvent(Object source, String appName, String serverId,
			InstanceInfo instanceInfo, boolean replication) {
		super(source);
		this.appName = appName;
		this.serverId = serverId;
		this.instanceInfo = instanceInfo;
		this.replication = replication;
	}
}
