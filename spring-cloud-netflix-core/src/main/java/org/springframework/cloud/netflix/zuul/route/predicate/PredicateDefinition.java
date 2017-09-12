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

package org.springframework.cloud.netflix.zuul.route.predicate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import org.springframework.cloud.netflix.zuul.util.NameUtils;
import org.springframework.validation.annotation.Validated;

import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * @author Spencer Gibb
 */
@Validated
public class PredicateDefinition {
	@NotNull
	private String name;
	private Map<String, String> args = new LinkedHashMap<>();

	public PredicateDefinition() {
	}

	public PredicateDefinition(String text) {
		int eqIdx = text.indexOf("=");
		if (eqIdx <= 0) {
			throw new ValidationException("Unable to parse PredicateDefinition text '" + text + "'" +
					", must be of the form name=value");
		}
		setName(text.substring(0, eqIdx));

		String[] args = tokenizeToStringArray(text.substring(eqIdx+1), ",");

		for (int i=0; i < args.length; i++) {
			this.args.put(NameUtils.generateName(i), args[i]);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getArgs() {
		return args;
	}

	public void setArgs(Map<String, String> args) {
		this.args = args;
	}

	public void addArg(String key, String value) {
		this.args.put(key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PredicateDefinition that = (PredicateDefinition) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(args, that.args);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, args);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("PredicateDefinition{");
		sb.append("name='").append(name).append('\'');
		sb.append(", args=").append(args);
		sb.append('}');
		return sb.toString();
	}
}
