package org.springframework.cloud.netflix.feign;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.ribbon.LoadBalancingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.netflix.archaius.ConfigurableEnvironmentConfiguration;

import java.net.URI;

/**
 * @author Spencer Gibb
 */
@Configuration
public class FeignConfigurer {
    @Autowired
    ConfigurableEnvironmentConfiguration envConfig; //FIXME: howto enforce this?

    @Autowired
    RibbonClientPreprocessor ribbonClientPreprocessor;

    @Autowired
    Decoder decoder;

    @Autowired
    Encoder encoder;

    @Autowired
    Logger logger;

    @Autowired
    Contract contract;

    @Autowired(required = false)
    Client ribbonClient;

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
        ribbonClientPreprocessor.preprocess(name);

        if(ribbonClient != null) {
            return builder.client(ribbonClient).target(type, schemeName);
        } else {
            return builder.target(LoadBalancingTarget.create(type, schemeName));
        }
    }

}
