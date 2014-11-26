package org.springframework.cloud.netflix.zuul;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author Spencer Gibb
 */
@Slf4j
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping implements ApplicationListener<InstanceRegisteredEvent> {

    @Autowired
    protected RouteLocator routeLocator;

    @Autowired
    protected ZuulController zuul;

    @Autowired
    protected ZuulProperties properties;

    public ZuulHandlerMapping() {
        setOrder(-200);
    }

    @PostConstruct
    public void init() {
    }

    @Override
    public void onApplicationEvent(InstanceRegisteredEvent event) {
        registerHandlers(routeLocator.getRoutes());
    }

    private void registerHandlers(Map<String, String> routes) {
        if (routes.isEmpty()) {
            logger.warn("Neither 'urlMap' nor 'mappings' set on SimpleUrlHandlerMapping");
        }
        else {
            for (Map.Entry<String, String> entry : routes.entrySet()) {
                String url = entry.getKey();
                // Prepend with slash if not already present.
                if (!url.startsWith("/")) {
                    url = "/" + url;
                }

                if (StringUtils.hasText(properties.getRoutePrefix())) {
                    url = properties.getMapping()+url;
                    if (!url.startsWith("/")) {
                        url = "/" + url;
                    }
                }

                registerHandler(url, zuul);
            }
        }
    }

}
