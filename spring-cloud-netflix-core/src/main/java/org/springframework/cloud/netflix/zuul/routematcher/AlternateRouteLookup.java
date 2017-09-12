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

package org.springframework.cloud.netflix.zuul.routematcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.cloud.netflix.zuul.route.predicate.ArgumentHints;
import org.springframework.cloud.netflix.zuul.route.predicate.PredicateDefinition;
import org.springframework.cloud.netflix.zuul.route.predicate.RoutePredicateFactory;
import org.springframework.cloud.netflix.zuul.util.NameUtils;
import org.springframework.tuple.Tuple;
import org.springframework.tuple.TupleBuilder;

import com.google.common.base.Predicate;

/**
 * Look up alternate route matching the predicate(s).
 * 
 * @author Mustansar Anwar
 *
 */
public class AlternateRouteLookup {

	private static final Log log = LogFactory
			.getLog(AlternateRouteLookup.class);

	private final Map<String, RoutePredicateFactory> predicateFactories = new LinkedHashMap<>();

	public AlternateRouteLookup(List<RoutePredicateFactory> predicateFactoryList) {
		initFactories(predicateFactoryList);
	}

	private void initFactories(List<RoutePredicateFactory> predicateFactoryList) {
		for (RoutePredicateFactory factory : predicateFactoryList) {
			String key = factory.name();
			if (this.predicateFactories.containsKey(key)) {
				log.warn("A RoutePredicateFactory named " + key
						+ " already exists, class: " + this.predicateFactories.get(key)
						+ ". It will be overwritten.");
			}
			this.predicateFactories.put(key, factory);
			if (log.isInfoEnabled()) {
				log.info("Loaded RoutePredicateFactory [" + key + "]");
			}
		}
	}

	public AlternateRoute lookupAlternateRoute(ZuulRoute zuulRoute,
			HttpServletRequest request) {

		List<AlternateRoute> alternateRouteList = getAlternateRoute(
				zuulRoute.getRouteOptions());
		for (AlternateRoute alternateRoute : alternateRouteList) {
			if (alternateRoute != null) {
				if (isMatch(alternateRoute.getPredicates(), request)) {
					if (log.isDebugEnabled()) {
						log.debug("AlternateRoute matched: "
								+ alternateRoute.getId() + ": "
								+ alternateRoute.getUri());
					}
					return alternateRoute;
				}
			}
		}
		return null;
	}

	public boolean isMatch(List<Predicate<HttpServletRequest>> predicateList,
			HttpServletRequest request) {
		for (Predicate<HttpServletRequest> predicate : predicateList) {
			if (!predicate.apply(request)) {
				return false;
			}
		}
		return true;
	}

	protected List<AlternateRoute> getAlternateRoute(
			RouteOptions routeOptions) {

		List<RouteCondition> routeConditionsList = routeOptions
				.getRouteConditions();

		List<AlternateRoute> alternateRouteList = new ArrayList<>();
		for (RouteCondition routeCondition : routeConditionsList) {
			AlternateRoute alternateRoute = convertToAlternateRoute(
					routeCondition);
			if (alternateRoute != null) {
				alternateRouteList.add(alternateRoute);
			}

		}
		return alternateRouteList;
	}

	protected AlternateRoute convertToAlternateRoute(
			RouteCondition routeCondition) {
		if (routeCondition != null && !routeCondition.getPredicates().isEmpty()) {
			List<Predicate<HttpServletRequest>> predicateList = combinePredicates(
					routeCondition);

			return AlternateRoute.builder(routeCondition).predicates(predicateList)
					.build();
		}
		return null;
	}

	protected List<Predicate<HttpServletRequest>> combinePredicates(
			RouteCondition routeCondition) {

		List<Predicate<HttpServletRequest>> predicateList = new ArrayList<>();

		List<PredicateDefinition> predicatesDev = routeCondition.getPredicates();
		Predicate<HttpServletRequest> predicate = lookup(routeCondition,
				predicatesDev.get(0));
		predicateList.add(predicate);

		for (PredicateDefinition andPredicate : predicatesDev.subList(1,
				predicatesDev.size())) {
			Predicate<HttpServletRequest> found = lookup(routeCondition,
					andPredicate);
			predicateList.add(found);
		}

		return predicateList;
	}

	protected Predicate<HttpServletRequest> lookup(RouteCondition routeCondition,
			PredicateDefinition predicate) {
		RoutePredicateFactory found = this.predicateFactories.get(predicate.getName());
		if (found == null) {
			throw new IllegalArgumentException(
					"Unable to find RoutePredicateFactory with name "
							+ predicate.getName());
		}
		Map<String, String> args = predicate.getArgs();
		if (log.isDebugEnabled()) {
			log.debug("RouteDefinition " + routeCondition.getId() + " applying "
					+ args + " to " + predicate.getName());
		}

		Tuple tuple = getTuple(found, args);

		return found.apply(tuple);
	}

	protected Tuple getTuple(ArgumentHints hasArguments,
			Map<String, String> args) {
		TupleBuilder builder = TupleBuilder.tuple();

		List<String> argNames = hasArguments.argNames();
		if (!argNames.isEmpty()) {
			// ensure size is the same for key replacement later
			if (hasArguments.validateArgs() && args.size() != argNames.size()) {
				throw new IllegalArgumentException(
						"Wrong number of arguments. Expected " + argNames + " "
								+ argNames + ". Found " + args.size() + " "
								+ args + "'");
			}
		}

		int entryIdx = 0;
		for (Map.Entry<String, String> entry : args.entrySet()) {
			String key = entry.getKey();

			// RoutePredicateFactory has name hints and this has a fake key name
			// replace with the matching key hint
			if (key.startsWith(NameUtils.GENERATED_NAME_PREFIX)
					&& !argNames.isEmpty() && entryIdx < args.size()) {
				key = argNames.get(entryIdx);
			}

			builder.put(key, entry.getValue());
			entryIdx++;
		}

		Tuple tuple = builder.build();

		if (hasArguments.validateArgs()) {
			for (String name : argNames) {
				if (!tuple.hasFieldName(name)) {
					throw new IllegalArgumentException(
							"Missing argument '" + name + "'. Given " + tuple);
				}
			}
		}
		return tuple;
	}
}
