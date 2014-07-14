package filters.pre

import com.netflix.zuul.context.RequestContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.spring.platform.netflix.zuul.Routes
import io.spring.platform.netflix.zuul.SpringFilter

class PreDecorationFilter extends SpringFilter {
    private static Logger LOG = LoggerFactory.getLogger(PreDecorationFilter.class);

    @Override
    int filterOrder() {
        return 5
    }

    @Override
    String filterType() {
        return "pre"
    }

    @Override
    boolean shouldFilter() {
        return true;
    }

    @Override
    Object run() {
        RequestContext ctx = RequestContext.getCurrentContext()

        def requestURI = ctx.getRequest().getRequestURI()

        Routes routes = getBean(Routes.class)

        def routesMap = routes.getRoutes()
        def route = routesMap.keySet().find { path ->
            //TODO: use ant matchers?
            if (requestURI.startsWith(path)) {
                return true
            }
            return false
        }
        def serviceId = routesMap.get(route)

        if (serviceId != null) {
            // set serviceId for use in filters.route.RibbonRequest
            ctx.set("serviceId", serviceId)
            ctx.setRouteHost(null)
            ctx.addOriginResponseHeader("X-Zuul-ServiceId", serviceId);
        }
    }
}
