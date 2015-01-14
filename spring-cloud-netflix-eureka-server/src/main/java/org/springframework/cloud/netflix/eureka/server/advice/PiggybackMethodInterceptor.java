/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.cloud.netflix.eureka.server.advice;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 *
 */
public class PiggybackMethodInterceptor implements MethodInterceptor {

	private Object delegate;
	private Class<?>[] types;

	public PiggybackMethodInterceptor(Object delegate, Class<?>... types) {
		this.delegate = delegate;
		this.types = types;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object result = invocation.proceed();
		invokeAfter(invocation.getMethod(), invocation.getArguments());
		return result;
	}

	private void invokeAfter(Method method, Object[] arguments) throws Exception {
		for (Class<?> type : this.types) {
			Method target = getTarget(type, method);
			if (target != null) {
				target.invoke(this.delegate, arguments);
				return;
			}
		}
	}

	private Method getTarget(Class<?> type, Method method) {
		Method target = ReflectionUtils.findMethod(type, method.getName(),
				method.getParameterTypes());
		return target;
	}

}
