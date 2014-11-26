package org.springframework.cloud.netflix.zuul;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * @author Spencer Gibb
 */
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping implements ApplicationListener<InstanceRegisteredEvent> {

    private RouteLocator routeLocator;

    private ZuulController zuul;

    private ZuulProperties properties;

    @Autowired
    public ZuulHandlerMapping(RouteLocator routeLocator, ZuulController zuul, ZuulProperties properties) {
        this.routeLocator = routeLocator;
		this.zuul = zuul;
		this.properties = properties;
		setOrder(-200);
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
