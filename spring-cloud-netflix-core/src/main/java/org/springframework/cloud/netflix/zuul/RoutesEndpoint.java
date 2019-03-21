/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Endpoint to display and reset the zuul proxy routes
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@ManagedResource(description = "Can be used to list and reset the reverse proxy routes")
public class RoutesEndpoint implements MvcEndpoint, ApplicationEventPublisherAware {

	private ProxyRouteLocator routes;

	private ApplicationEventPublisher publisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Autowired
	public RoutesEndpoint(ProxyRouteLocator routes) {
		this.routes = routes;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	@ManagedOperation
	public Map<String, String> reset() {
		this.publisher.publishEvent(new RoutesRefreshedEvent(this.routes));
		return getRoutes();
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@ManagedAttribute
	public Map<String, String> getRoutes() {
		return this.routes.getRoutes();
	}

	@Override
	public String getPath() {
		return "/routes";
	}

	@Override
	public boolean isSensitive() {
		return true;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}
