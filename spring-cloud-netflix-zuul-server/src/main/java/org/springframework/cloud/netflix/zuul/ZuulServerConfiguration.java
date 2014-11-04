package org.springframework.cloud.netflix.zuul;

import com.netflix.zuul.http.ZuulServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(ZuulServlet.class)
@EnableConfigurationProperties(ZuulServerProperties.class)
@ConditionalOnExpression("${zuul.server.enabled:true}")
public class ZuulServerConfiguration extends AbstractZuulConfiguration {

    @Bean
    public ZuulServerProperties zuulServerProperties() {
        return new ZuulServerProperties();
    }

    @Bean
    //ZuulServerProperties doesn't implement ZuulProperties so there are not 2 implementations (see ZuulProxyConfiguration
    public ZuulProperties zuulProperties() {
        return new ZuulProperties() {
            @Override
            public String getMapping() {
                return zuulServerProperties().getMapping();
            }

            @Override
            public boolean isStripMapping() {
                return zuulServerProperties().isStripMapping();
            }

            @Override
            public String getRoutePrefix() {
                return zuulServerProperties().getRoutePrefix();
            }

            @Override
            public boolean isAddProxyHeaders() {
                return zuulServerProperties().isAddProxyHeaders();
            }
        };
    }

    @Override
    protected ZuulProperties getProperties() {
        return zuulProperties();
    }
}
