package org.springframework.cloud.netflix.eureka.server.event;

import static com.google.common.collect.Iterables.tryFind;

import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.advice.LeaseManagerLite;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.PeerAwareInstanceRegistry;
import com.netflix.eureka.lease.Lease;

/**
 * @author Spencer Gibb
 */
public class LeaseManagerMessageBroker implements LeaseManagerLite<InstanceInfo> {
    private static final Logger logger = LoggerFactory.getLogger(LeaseManagerMessageBroker.class);

    @Autowired
    private ApplicationContext ctxt;
    
    @Override
    public void register(InstanceInfo info, boolean isReplication) {
    	register(info, Lease.DEFAULT_DURATION_IN_SECS, isReplication);
    }

    @Override
    public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
        logger.debug("register {}, vip {}, leaseDuration {}, isReplication {}",
                info.getAppName(), info.getVIPAddress(), leaseDuration, isReplication);
        //TODO: what to publish from info (whole object?)
        ctxt.publishEvent(new EurekaInstanceRegisteredEvent(this, info, leaseDuration, isReplication));
    }

    @Override
    public boolean cancel(String appName, String serverId, boolean isReplication) {
        logger.debug("cancel {}, serverId {}, isReplication {}", appName, serverId, isReplication);
        ctxt.publishEvent(new EurekaInstanceCanceledEvent(this, appName, serverId, isReplication));
        return false;
    }

    @Override
    public boolean renew(final String appName, final String serverId, boolean isReplication) {
        logger.debug("renew {}, serverId {}, isReplication {}", appName, serverId, isReplication);
        List<Application> applications = PeerAwareInstanceRegistry.getInstance().getSortedApplications();
        Optional<Application> app = tryFind(applications, new Predicate<Application>() {
            @Override
            public boolean apply(@Nullable Application input) {
                return input.getName().equals(appName);
            }
        });

        if (app.isPresent()) {
            Optional<InstanceInfo> info = tryFind(app.get().getInstances(), new Predicate<InstanceInfo>() {
                @Override
                public boolean apply(@Nullable InstanceInfo input) {
                    return input.getHostName().equals(serverId);
                }
            });
            ctxt.publishEvent(new EurekaInstanceRenewedEvent(this, appName, serverId, info.orNull(), isReplication));
        }
        return false;
    }

    @Override
    public void evict() {}
}
