package org.springframework.platform.netflix.feign;

import com.netflix.config.ConfigurationManager;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.ribbon.LoadBalancingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.platform.netflix.archaius.ConfigurableEnvironmentConfiguration;

import java.net.URI;

/**
 * Created by sgibb on 7/3/14.
 */
@Configuration
public class FeignConfigurer {
    @Autowired
    ConfigurableEnvironmentConfiguration envConfig; //FIXME: howto enforce this?

    @Autowired
    Decoder decoder;

    @Autowired
    Encoder encoder;

    @Autowired
    Logger logger;

    @Autowired
    Contract contract;

    protected Feign.Builder feign() {
        //ConfigurationManager.getConfigInstance().setProperty("exampleBackend.ribbon.listOfServers", "localhost:7080");
        //exampleBackend.ribbon.NIWSServerListClassName=my.package.MyServerList
        return Feign.builder()
                .logger(logger)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract);
    }

    protected <T> T loadBalance(Class<T> type, String schemeName) {
        return loadBalance(feign(), type, schemeName);
    }

    protected <T> T loadBalance(Feign.Builder builder, Class<T> type, String schemeName) {
        String name = URI.create(schemeName).getHost();
        setServiceListClass(name);
        return builder.target(LoadBalancingTarget.create(type, schemeName));
    }

    public static void setServiceListClass(String serviceId) {
        setProp(serviceId, "NIWSServerListClassName", DiscoveryEnabledNIWSServerList.class.getName());
        setProp(serviceId, "DeploymentContextBasedVipAddresses", serviceId); //FIXME: what should this be?
    }

    private static void setProp(String serviceId, String suffix, String value) {
        ConfigurationManager.getConfigInstance().setProperty(serviceId + ".ribbon." + suffix, value);
    }
}
