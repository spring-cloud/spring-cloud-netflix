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

import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Endpoint used to reset the reverse proxy routes
 * @author Ryan Baxter
 */
@ManagedResource(description = "Can be used to reset the reverse proxy routes")
public class RoutesMvcEndpoint extends EndpointMvcAdapter implements ApplicationEventPublisherAware {

	private RouteLocator routes;
	private ApplicationEventPublisher publisher;

	public RoutesMvcEndpoint(RoutesEndpoint endpoint, RouteLocator routes) {
		super(endpoint);
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
}
