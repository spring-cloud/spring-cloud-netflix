package org.springframework.cloud.netflix.eureka;

import org.springframework.cloud.client.discovery.DiscoveryHeartbeatEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Specifically used when eureka is in the parent bootstrap context to relay the DiscoveryHeartbeatEvent to the child.  Avoids stack overflow
 * @author Spencer Gibb
 *
 * TODO: create a ParentDiscoveryHeartbeatEvent in s-c-commons rather than eureka, so eureka doesn't leak into zuul (so zuul can be used  with consul for example)
 */
@SuppressWarnings("serial")
public class EurekaHeartbeatEvent extends ApplicationEvent {

	private final Object value;

	public EurekaHeartbeatEvent(DiscoveryHeartbeatEvent e) {
		super(e.getSource());
		value = e.getValue();
	}

	public Object getValue() {
		return value;
	}
}
