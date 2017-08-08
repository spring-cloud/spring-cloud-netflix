/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

/**
 * Allows {@link ZuulRoute}s to be dynamically allowed or rejected by
 * {@link RouteLocator}s when advertising available {@link Route}s.
 *
 * @author Tom Cawley
 */
public interface RouteLocatorFilter {

	/**
	 * Return {@code true} to accept (allow) the route, {@code false} otherwise.

	 * @param route The {@link ZuulRoute} to inspect.
	 * @return {@code true} to accept the route, {@code false} otherwise.
	 */
	boolean acceptRoute(ZuulRoute route);
}
