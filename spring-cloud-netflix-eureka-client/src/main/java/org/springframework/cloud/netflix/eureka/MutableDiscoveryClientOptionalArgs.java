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

package org.springframework.cloud.netflix.eureka;

import java.util.Collection;
import java.util.LinkedHashSet;

import com.netflix.discovery.DiscoveryClient.DiscoveryClientOptionalArgs;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * @author Dave Syer
 */
public class MutableDiscoveryClientOptionalArgs extends DiscoveryClientOptionalArgs {

	private Collection<ClientFilter> additionalFilters;

	@Override
	public void setAdditionalFilters(Collection<ClientFilter> additionalFilters) {
		additionalFilters = new LinkedHashSet<ClientFilter>(additionalFilters);
		this.additionalFilters = additionalFilters;
		super.setAdditionalFilters(additionalFilters);
	}

	public Collection<ClientFilter> getAdditionalFilters() {
		return this.additionalFilters;
	}

}
