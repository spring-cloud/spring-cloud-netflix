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

package org.springframework.cloud.netflix.feign.hystrix;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Daniel Lavoie
 */
public class SecurityContextHystrixRequestVariableSetterFilter implements Filter {
	private static Logger LOGGER = LoggerFactory
			.getLogger(SecurityContextHystrixRequestVariableSetterFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		SecurityContext context = SecurityContextHolder.getContext();

		if (context != null) {
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Storing security context for hystrix command : context = "
						+ context);

			SecurityContextHystrixRequestVariable.getInstance()
					.set(SecurityContextHolder.getContext());
		}
		else if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Security context is undefined.");
		}

		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}
