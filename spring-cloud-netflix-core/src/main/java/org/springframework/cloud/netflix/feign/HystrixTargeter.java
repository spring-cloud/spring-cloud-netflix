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
 *
 */

package org.springframework.cloud.netflix.feign;

import feign.Feign;
import feign.Target;

/**
 * @author Spencer Gibb
 */
@SuppressWarnings("unchecked")
public class HystrixTargeter implements Targeter {

	@Override
	public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignContext context,
						Target.HardCodedTarget<T> target) {
		if (factory.getFallback() == void.class
				|| !(feign instanceof feign.hystrix.HystrixFeign.Builder)) {
			return feign.target(target);
		}

		Object fallbackInstance = context.getInstance(factory.getName(), factory.getFallback());
		if (fallbackInstance == null) {
			throw new IllegalStateException(String.format(
					"No fallback instance of type %s found for feign client %s",
					factory.getFallback(), factory.getName()));
		}

		if (!target.type().isAssignableFrom(factory.getFallback())) {
			throw new IllegalStateException(
					String.format(
							"Incompatible fallback instance. Fallback of type %s is not assignable to %s for feign client %s",
							factory.getFallback(), target.type(), factory.getName()));
		}

		feign.hystrix.HystrixFeign.Builder builder = (feign.hystrix.HystrixFeign.Builder) feign;
		return builder.target(target, (T) fallbackInstance);
	}
}
