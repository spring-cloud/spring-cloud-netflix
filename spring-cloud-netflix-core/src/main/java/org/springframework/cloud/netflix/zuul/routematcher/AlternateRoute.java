/*
 * Copyright 2013-2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.zuul.routematcher;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import com.google.common.base.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * @author Spencer Gibb
 * @author Mustansar Anwar
 */
public class AlternateRoute implements Ordered {

	private final String id;

	private final URI uri;

	private final int order;

	private final List<Predicate<HttpServletRequest>> predicates;

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(RouteCondition routeCondition) {
		return new Builder()
				.id(routeCondition.getId())
				.uri(routeCondition.getUrl())
				.order(routeCondition.getOrder());
	}

	public AlternateRoute(String id, URI uri, int order, List<Predicate<HttpServletRequest>> predicates) {
		this.id = id;
		this.uri = uri;
		this.order = order;
		this.predicates = predicates;
	}

	public static class Builder {
		private String id;

		private URI uri;

		private int order = 0;

		private List<Predicate<HttpServletRequest>> predicates;

		private Builder() {}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder uri(String uri) {
			this.uri = URI.create(uri);
			return this;
		}

		public Builder order(int order) {
			this.order = order;
			return this;
		}

		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder predicates(List<Predicate<HttpServletRequest>> predicates) {
			this.predicates = predicates;
			return this;
		}

		public AlternateRoute build() {
			Assert.notNull(this.id, "id can not be null");
			Assert.notNull(this.uri, "uri can not be null");
			//TODO: Assert.notNull(this.predicate, "predicate can not be null");

			return new AlternateRoute(this.id, this.uri, this.order, this.predicates);
		}
	}

	public String getId() {
		return this.id;
	}

	public URI getUri() {
		return this.uri;
	}

	public int getOrder() {
		return order;
	}

	public List<Predicate<HttpServletRequest>> getPredicates() {
		return this.predicates;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AlternateRoute route = (AlternateRoute) o;
		return Objects.equals(id, route.id) &&
				Objects.equals(uri, route.uri) &&
				Objects.equals(order, route.order) &&
				Objects.equals(predicates, route.predicates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, uri, predicates);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("AlternateRoute{");
		sb.append("id='").append(id).append('\'');
		sb.append(", uri=").append(uri);
		sb.append(", order=").append(order);
		sb.append(", predicates=").append(predicates);
		sb.append('}');
		return sb.toString();
	}
}
