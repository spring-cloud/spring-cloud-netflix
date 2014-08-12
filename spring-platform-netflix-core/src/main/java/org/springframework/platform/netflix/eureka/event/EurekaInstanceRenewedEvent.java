package org.springframework.platform.netflix.eureka.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 */
@Data
public class EurekaInstanceRenewedEvent extends ApplicationEvent {
    private String appName;
    private String serverId;
    boolean replication;

    public EurekaInstanceRenewedEvent(Object source, String appName, String serverId, boolean replication) {
        super(source);
        this.appName = appName;
        this.serverId = serverId;
        this.replication = replication;
    }
}
