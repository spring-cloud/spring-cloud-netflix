/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.http;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;

/**
 * A simple wrapper class for {@link Applications} that insure proprer Jackson
 * serialization through the JsonPropert overwrites.
 * 
 * @author Daniel Lavoie
 */
public class EurekaApplications extends com.netflix.discovery.shared.Applications {

	@JsonCreator
	public EurekaApplications(@JsonProperty("apps__hashcode") String appsHashCode,
			@JsonProperty("versions__delta") Long versionDelta,
			@JsonProperty("application") List<Application> registeredApplications) {
		super(appsHashCode, versionDelta, registeredApplications);
	}
}