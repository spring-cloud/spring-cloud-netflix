package org.springframework.cloud.netflix.eureka;

import org.springframework.beans.factory.annotation.Autowired;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.EurekaClientConfig;

/**
 * @author Spencer Gibb
 */
public class DiscoveryManagerInitializer {

	@Autowired
	private EurekaClientConfig clientConfig;

	@Autowired
	private EurekaInstanceConfig instanceConfig;

	public synchronized void init() {
		if (DiscoveryManager.getInstance().getDiscoveryClient() == null) {
			DiscoveryManager.getInstance().initComponent(this.instanceConfig,
					this.clientConfig);
		}
	}
}
