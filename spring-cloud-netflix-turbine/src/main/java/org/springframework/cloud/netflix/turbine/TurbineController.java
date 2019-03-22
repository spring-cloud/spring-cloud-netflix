/*
 * Copyright 2018-2019 the original author or authors.
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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Defines endpoints to use with the Turbine.
 *
 * @author Anastasiia Smirnova
 */
@RestController
public class TurbineController {

	private final TurbineInformationService turbineInformationService;

	public TurbineController(TurbineInformationService turbineInformationService) {
		this.turbineInformationService = turbineInformationService;
	}

	@GetMapping(value = "/clusters", produces = MediaType.APPLICATION_JSON_VALUE)
	public Collection<ClusterInformation> clusters(HttpServletRequest request) {
		return turbineInformationService.getClusterInformations(request);
	}

}
