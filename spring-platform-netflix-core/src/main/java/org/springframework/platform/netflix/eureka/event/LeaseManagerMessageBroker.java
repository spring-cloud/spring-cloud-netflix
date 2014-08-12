package org.springframework.platform.netflix.eureka.event;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.lease.LeaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * @author Spencer Gibb
 */
public class LeaseManagerMessageBroker implements LeaseManager<InstanceInfo> {
    private static final Logger logger = LoggerFactory.getLogger(LeaseManagerMessageBroker.class);

    @Autowired
    private ApplicationContext ctxt;

    @Override
    public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
        logger.debug("register {}, vip {}, leaseDuration {}, isReplication {}",
                info.getAppName(), info.getVIPAddress(), leaseDuration, isReplication);
        //TODO: what to publish from info (whole object?)
        ctxt.publishEvent(new EurekaInstanceRegisteredEvent(this, info.getAppName(),
                info.getVIPAddress(), leaseDuration, isReplication));
    }

    @Override
    public boolean cancel(String appName, String serverId, boolean isReplication) {
        logger.debug("cancel {}, serverId {}, isReplication {}", appName, serverId, isReplication);
        ctxt.publishEvent(new EurekaInstanceCanceledEvent(this, appName, serverId, isReplication));
        return false;
    }

    @Override
    public boolean renew(String appName, String serverId, boolean isReplication) {
        logger.debug("renew {}, serverId {}, isReplication {}", appName, serverId, isReplication);
        ctxt.publishEvent(new EurekaInstanceRenewedEvent(this, appName, serverId, isReplication));
        return false;
    }

    @Override
    public void evict() {}
}
