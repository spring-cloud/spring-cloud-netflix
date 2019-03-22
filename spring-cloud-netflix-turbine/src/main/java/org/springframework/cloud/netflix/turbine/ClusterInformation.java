/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.turbine;

import java.util.Objects;

/**
 * @author Anastasiia Smirnova
 * @author Ryan Baxter Contains cluster-relevant information, such as name and link.
 */
public class ClusterInformation {

	private String name;

	private String link;

	public ClusterInformation() {
	}

	public ClusterInformation(String name, String link) {
		this.name = name;
		this.link = link;
	}

	public String getName() {
		return name;
	}

	public String getLink() {
		return link;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClusterInformation that = (ClusterInformation) o;
		return Objects.equals(name, that.name) && Objects.equals(link, that.link);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, link);
	}

	@Override
	public String toString() {
		return "ClusterInformation{" + "name='" + name + '\'' + ", link='" + link + '\''
				+ '}';
	}

}
