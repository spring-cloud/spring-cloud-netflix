package org.springframework.cloud.netflix.turbine;

import com.netflix.turbine.discovery.InstanceDiscovery;
import com.netflix.turbine.init.TurbineInit;
import com.netflix.turbine.plugins.PluginsFactory;
import com.netflix.turbine.streaming.servlet.TurbineStreamServlet;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableEurekaClient
public class TurbineConfiguration implements SmartLifecycle, Ordered {

    @Bean
    public ServletRegistrationBean turbineStreamServlet() {
        return new ServletRegistrationBean(new TurbineStreamServlet(), "/turbine.stream");
    }

    @Bean
    public InstanceDiscovery instanceDiscovery() {
        return new EurekaInstanceDiscovery();
    }

    private boolean running;

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        callback.run();
    }

    @Override
    public void start() {
        //TODO: figure out ordering, so this is already run by EurekaClientConfiguration
        //DiscoveryManager.getInstance().initComponent(instanceConfig, clientConfig);
        PluginsFactory.setClusterMonitorFactory(new SpringAggregatorFactory());
        PluginsFactory.setInstanceDiscovery(instanceDiscovery());
        TurbineInit.init();
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
