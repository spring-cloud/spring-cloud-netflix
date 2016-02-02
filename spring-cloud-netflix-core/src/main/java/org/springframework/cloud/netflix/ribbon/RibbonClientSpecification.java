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

package org.springframework.cloud.netflix.ribbon;

import org.springframework.cloud.context.named.NamedContextFactory;

/**
 * @author Dave Syer
 */
public class RibbonClientSpecification implements NamedContextFactory.Specification {

	private String name;

	private Class<?>[] configuration;

	public RibbonClientSpecification(String name, Class<?>[] configuration) {
		this.name = name;
		this.configuration = configuration;
	}

	public RibbonClientSpecification() {
	}

	public String getName() {
		return this.name;
	}

	public Class<?>[] getConfiguration() {
		return this.configuration;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setConfiguration(Class<?>[] configuration) {
		this.configuration = configuration;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RibbonClientSpecification))
			return false;
		final RibbonClientSpecification other = (RibbonClientSpecification) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$name = this.name;
		final Object other$name = other.name;
		if (this$name == null ? other$name != null : !this$name.equals(other$name))
			return false;
		if (!java.util.Arrays.deepEquals(this.configuration, other.configuration))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.name;
		result = result * PRIME + ($name == null ? 0 : $name.hashCode());
		result = result * PRIME + java.util.Arrays.deepHashCode(this.configuration);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof RibbonClientSpecification;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.ribbon.RibbonClientSpecification(name="
				+ this.name + ", configuration=" + java.util.Arrays
				.deepToString(this.configuration) + ")";
	}
}
