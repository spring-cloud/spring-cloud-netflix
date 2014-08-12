package org.springframework.platform.netflix.eureka.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 */
@Data
public class EurekaInstanceRegisteredEvent extends ApplicationEvent {
    private String appName;
    private String vip;
    private int leaseDuration;
    boolean replication;

    public EurekaInstanceRegisteredEvent(Object source, String appName, String vip, int leaseDuration, boolean replication) {
        super(source);
        this.appName = appName;
        this.vip = vip;
        this.leaseDuration = leaseDuration;
        this.replication = replication;
    }
}
