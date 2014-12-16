package org.springframework.cloud.netflix.zuul;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter;
import org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.DebugFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.Servlet30WrapperFilter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.http.ZuulServlet;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties(ZuulProperties.class)
@ConditionalOnClass(ZuulServlet.class)
public class ZuulConfiguration {

	@Autowired
	private ZuulProperties zuulProperties;

	@Bean
	public RouteLocator routes() {
		return new SimpleRouteLocator(zuulProperties);
	}

	@Bean
	public ZuulController zuulController() {
		return new ZuulController();
	}

	@Bean
	public ZuulHandlerMapping zuulHandlerMapping(RouteLocator routes) {
		return new ZuulHandlerMapping(routes, zuulController());
	}

	@Configuration
	protected static class ZuulFilterConfiguration {

		@Autowired
		private Map<String, ZuulFilter> filters;

		@Bean
		public FilterInitializer zuulFilterInitializer() {
			return new FilterInitializer(filters);
		}

	}

	@Bean
	public ApplicationListener<ApplicationEvent> zuulRefreshRoutesListener() {
		return new ZuulRefreshListener();
	}

	private static class ZuulRefreshListener implements
			ApplicationListener<ApplicationEvent> {

		@Autowired
		private ZuulHandlerMapping zuulHandlerMapping;

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextRefreshedEvent
					|| event instanceof RefreshScopeRefreshedEvent)
				zuulHandlerMapping.registerHandlers();
		}

	}

	// pre filters
	@Bean
	public DebugFilter debugFilter() {
		return new DebugFilter();
	}

	@Bean
	public Servlet30WrapperFilter servlet30WrapperFilter() {
		return new Servlet30WrapperFilter();
	}

	// post filters
	@Bean
	public SendResponseFilter sendResponseFilter() {
		return new SendResponseFilter();
	}

	@Bean
	public SendErrorFilter sendErrorFilter() {
		return new SendErrorFilter();
	}

}
