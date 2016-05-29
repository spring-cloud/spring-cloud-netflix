/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.feign.hystrix.app;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * @author Daniel Lavoie
 */
@Component
public class TestInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate template) {
		if (SecurityContextHolder.getContext().getAuthentication() != null)
			template.header("username",
					SecurityContextHolder.getContext().getAuthentication().getName());
	}
}
