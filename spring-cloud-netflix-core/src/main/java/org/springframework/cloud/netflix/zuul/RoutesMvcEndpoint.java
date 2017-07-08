/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul;

import org.springframework.boot.actuate.endpoint.mvc.ActuatorMediaTypes;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.MediaType;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Endpoint used to reset the reverse proxy routes
 * @author Ryan Baxter
 * @author Gregor Zurowski
 */
@ManagedResource(description = "Can be used to reset the reverse proxy routes")
public class RoutesMvcEndpoint extends EndpointMvcAdapter implements ApplicationEventPublisherAware {

	static final String FORMAT_DETAILS = "details";

	private final RoutesEndpoint endpoint;
	private RouteLocator routes;
	private ApplicationEventPublisher publisher;

	public RoutesMvcEndpoint(RoutesEndpoint endpoint, RouteLocator routes) {
		super(endpoint);
		this.endpoint = endpoint;
		this.routes = routes;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	@ManagedOperation
	public Object reset() {
		this.publisher.publishEvent(new RoutesRefreshedEvent(this.routes));
		return super.invoke();
	}

	/**
	 * Expose Zuul {@link Route} information with details.
	 */
	@GetMapping(params = "format", produces = { ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
			MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public Object invokeRouteDetails(@RequestParam String format) {
		if (FORMAT_DETAILS.equalsIgnoreCase(format)) {
			return endpoint.invokeRouteDetails();
		} else {
			return super.invoke();
		}
	}
}