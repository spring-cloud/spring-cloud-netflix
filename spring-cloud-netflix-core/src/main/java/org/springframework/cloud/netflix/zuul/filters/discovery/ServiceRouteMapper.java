package org.springframework.cloud.netflix.zuul.filters.discovery;

import java.util.Set;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

/**
 * Provide a way to apply convention between routes and discovered services name.
 *
 * @author Stéphane LEROY
 * @author Saiyed Zaidi
 *
 */
public interface ServiceRouteMapper {

	/**
	 * Take a service Id (its discovered name) and return a route path.
	 *
	 * @param serviceId
	 *            service discovered name
	 * @deprecated Replaced by the {@link #applyRoute(String) applyRoute}
	 *             method.
	 * @return route path
	 */
	String apply(String serviceId);

	/**
	 * Take a service Id (its discovered name) and return a route.
	 *
	 * @param serviceId
	 *            service discovered name
	 * @return route
	 */
	DynamicRoute applyRoute(String serviceId);
	
	
	public class DynamicRoute extends ZuulRoute {

		public DynamicRoute(String path, String location, boolean stripPrefix, boolean retryable,
				Set<String> sensitiveHeaders) {
			super((path.startsWith("/") ? path.substring(1) : path).replace("/*", "").replace("*", ""), path, location,
					null, stripPrefix, retryable, sensitiveHeaders);
			this.path = path;
		}
		
		public DynamicRoute(String serviceId) {
			this(serviceId, serviceId, true, false, null);
		}

		private String path;
		
		public String getPath(){
			return path;
		}

	}
}
