/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.cloud.netflix.zuul.routematcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 
 * @author Mustansar Anwar
 *
 */
public class RouteOptions {

	private Set<String> allowedMethods = new HashSet<>();

	private List<RouteCondition> routeConditions = new ArrayList<>();

	public Set<String> getAllowedMethods() {
		return allowedMethods;
	}

	public void setAllowedMethods(Set<String> allowedMethods) {
		this.allowedMethods = allowedMethods;
	}

	public List<RouteCondition> getRouteConditions() {
		return routeConditions;
	}

	public void setRouteConditions(List<RouteCondition> routeConditions) {
		this.routeConditions = routeConditions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RouteOptions routeOptions = (RouteOptions) o;
		return Objects.equals(allowedMethods, routeOptions.allowedMethods)
				&& Objects.equals(routeConditions,
						routeOptions.routeConditions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(allowedMethods, routeConditions);
	}

	@Override
	public String toString() {
		return "RouteOptions{" + "allowedMethods='" + allowedMethods + '\''
				+ ", routeConditionList=" + routeConditions + '}';
	}

}
