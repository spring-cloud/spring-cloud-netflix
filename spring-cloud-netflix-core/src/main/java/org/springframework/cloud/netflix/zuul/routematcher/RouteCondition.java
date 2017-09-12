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

import static org.springframework.util.StringUtils.tokenizeToStringArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.cloud.netflix.zuul.route.predicate.PredicateDefinition;
import org.springframework.validation.annotation.Validated;


/**
 * @author Spencer Gibb
 * @author Mustansar Anwar
 */
@Validated
public class RouteCondition {
	@NotEmpty
	private String id = UUID.randomUUID().toString();

	@NotEmpty
	@Valid
	private List<PredicateDefinition> predicates = new ArrayList<>();

	@NotNull
	private URI url;

	private int order = 0;

	public RouteCondition() {}

	public RouteCondition(String text) {
		int eqIdx = text.indexOf("=");
		if (eqIdx <= 0) {
			throw new ValidationException("Unable to parse RouteCondition text '" + text + "'" +
					", must be of the form name=value");
		}

		setId(text.substring(0, eqIdx));

		String[] args = tokenizeToStringArray(text.substring(eqIdx+1), ",");

		setUrl(URI.create(args[0]));

		for (int i=1; i < args.length; i++) {
			this.predicates.add(new PredicateDefinition(args[i]));
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<PredicateDefinition> getPredicates() {
		return predicates;
	}

	public void setPredicates(List<PredicateDefinition> predicates) {
		this.predicates = predicates;
	}

	public URI getUrl() {
		return url;
	}

	public void setUrl(URI url) {
		this.url = url;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RouteCondition routeDefinition = (RouteCondition) o;
		return Objects.equals(id, routeDefinition.id) &&
				Objects.equals(predicates, routeDefinition.predicates) &&
				Objects.equals(order, routeDefinition.order) &&
				Objects.equals(url, routeDefinition.url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, predicates, url, order);
	}

	@Override
	public String toString() {
		return "RouteCondition{" +
				"id='" + id + '\'' +
				", predicates=" + predicates +
				", url=" + url +
				", order=" + order +
				'}';
	}
}