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
 */

package org.springframework.cloud.netflix.ribbon;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Rico Pahlisch
 * @author Gregor Zurowski
 */
@ConfigurationProperties("ribbon")
public class ServerIntrospectorProperties {

	private List<Integer> securePorts = Arrays.asList(443,8443);

	public List<Integer> getSecurePorts() {
		return securePorts;
	}

	public void setSecurePorts(List<Integer> securePorts) {
		this.securePorts = securePorts;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServerIntrospectorProperties that = (ServerIntrospectorProperties) o;
		return Objects.equals(securePorts, that.securePorts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(securePorts);
	}

	@Override
	public String toString() {
		return new StringBuilder("ServerIntrospectorProperties{")
				.append("securePorts=").append(securePorts)
				.append("}").toString();
	}

}
