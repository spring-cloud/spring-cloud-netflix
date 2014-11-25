package org.springframework.cloud.netflix.eureka;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Iterables.*;

/**
 * @author Spencer Gibb
 */
public class EurekaDiscoveryClient implements DiscoveryClient {

    @Autowired
    private EurekaInstanceConfigBean config;

    @Autowired
    private com.netflix.discovery.DiscoveryClient discovery;

    @Override
    public ServiceInstance getLocalServiceInstance() {
        return new ServiceInstance() {
            @Override
            public String getServiceId() {
                return config.getAppname();
            }

            @Override
            public String getHost() {
                return config.getHostname();
            }

            @Override
            public int getPort() {
                return config.getNonSecurePort();
            }
        };
    }

    @Override
    public List<String> getServices() {
        Applications applications = discovery.getApplications();
        if (applications == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(transform(applications.getRegisteredApplications(), new Function<Application, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Application app) {
                return app.getName().toLowerCase();
            }
        }));
    }
}
