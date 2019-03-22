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

package org.springframework.cloud.netflix.hystrix.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * @author Dave Syer
 */
@Controller
public class HystrixDashboardController {

	@RequestMapping("/hystrix")
	public String home(Model model, WebRequest request) {
		model.addAttribute("basePath", extractPath(request));
		return "hystrix/index";
	}

	@RequestMapping("/hystrix/{path}")
	public String monitor(@PathVariable String path, Model model, WebRequest request) {
		model.addAttribute("basePath", extractPath(request));
		model.addAttribute("contextPath", request.getContextPath());
		return "hystrix/" + path;
	}

	private String extractPath(WebRequest request) {
		String path = request.getContextPath() + request.getAttribute(
				"org.springframework."
						+ "web.servlet.HandlerMapping.pathWithinHandlerMapping",
				RequestAttributes.SCOPE_REQUEST);
		return path;
	}

}
