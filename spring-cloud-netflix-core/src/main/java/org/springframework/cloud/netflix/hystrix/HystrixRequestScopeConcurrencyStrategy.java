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
package org.springframework.cloud.netflix.hystrix;

import java.util.concurrent.Callable;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;

/**
 * Passes the request attributes used by Spring from the parent ThreadLocal (the one which
 * handles the request) to the child Thread (created by Hystrix). In fact, Hystrix
 * executes wrapped code in a different thread pool, so session and request scoped Spring
 * beans are not accessible; request and session attributes must be copied from the parent
 * ThreadLocal to the Hystrix ThreadLocal. See the bugs below:
 *
 * https://github.com/spring-cloud/spring-cloud-netflix/issues/1124
 * https://github.com/Netflix/Hystrix/issues/1162
 *
 *
 * Created by ctasso on 30/09/2016.
 *
 * @author Claudio Tasso
 */

public class HystrixRequestScopeConcurrencyStrategy extends HystrixConcurrencyStrategy {
	@Override
	public <T> Callable<T> wrapCallable(Callable<T> callable) {

		// retrieves current ThreadLocal request attributes
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

		return new WrapperCallable<T>(requestAttributes, callable);
	}

	private static class WrapperCallable<T> implements Callable<T> {

		private RequestAttributes requestAttributes;
		private Callable<T> target;

		WrapperCallable(RequestAttributes requestAttributes, Callable<T> target) {
			this.requestAttributes = requestAttributes;
			this.target = target;
		}

		@Override
		public T call() throws Exception {
			RequestContextHolder.setRequestAttributes(this.requestAttributes);

			try {
				return target.call();
			}
			finally {
				RequestContextHolder.resetRequestAttributes();
			}
		}
	}
}
