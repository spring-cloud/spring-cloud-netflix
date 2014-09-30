package org.springframework.cloud.netflix.eureka;

import com.netflix.discovery.DiscoveryManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Spencer Gibb
 */
public class DiscoveryManagerIntitializer {

    @Autowired
    private EurekaClientConfigBean clientConfig;

    @Autowired
    private EurekaInstanceConfigBean instanceConfig;


    public synchronized void init() {
        if (DiscoveryManager.getInstance().getDiscoveryClient() == null) {
            DiscoveryManager.getInstance().initComponent(instanceConfig, clientConfig);
        }
    }
}
