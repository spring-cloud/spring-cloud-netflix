package org.springframework.platform.netflix.eureka.event;

import com.netflix.appinfo.InstanceInfo;
import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 */
@Data
public class EurekaInstanceRenewedEvent extends ApplicationEvent {
    private String appName;
    private String serverId;
    private InstanceInfo instanceInfo;
    boolean replication;

    public EurekaInstanceRenewedEvent(Object source, String appName, String serverId, InstanceInfo instanceInfo, boolean replication) {
        super(source);
        this.appName = appName;
        this.serverId = serverId;
        this.instanceInfo = instanceInfo;
        this.replication = replication;
    }
}
