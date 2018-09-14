/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.cloud.netflix.concurrency.limits.web;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.Limiter.Listener;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ConcurrencyLimitsHandlerInterceptor implements HandlerInterceptor {

	private final Limiter<HttpServletRequest> limiter;

	public ConcurrencyLimitsHandlerInterceptor(Limiter<HttpServletRequest> limiter) {
		this.limiter = limiter;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		Optional<Listener> listener = limiter.acquire(request);
		if (listener.isPresent()) {
			request.setAttribute("concurrency_limiter_listener", listener.get());
			return true;
		}

		try {
			//TODO: headers with information?
			/*response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
			response.getWriter().print("Concurrency limit exceeded");*/
			response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
					"Concurrency limit exceeded");
		} catch (IOException e) {
			// ignore
		}
		return false;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		Listener listener = (Listener) request.getAttribute("concurrency_limiter_listener");
		if (listener != null) {
			if (ex != null) {
				listener.onIgnore();
			} else {
				listener.onSuccess();
			}
		}
	}

}
