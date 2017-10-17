/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.zuul.filters.route;

import org.springframework.http.client.ClientHttpResponse;

/**
 * Extension of {@link ZuulFallbackProvider} which adds possibility to choose proper response
 * based on the exception which caused the main method to fail.
 *
 * @author Dominik Mostek
 */
public interface FallbackProvider extends ZuulFallbackProvider {

	/**
	 * Provides a fallback response based on the cause of the failed execution.
	 *
	 * @param cause cause of the main method failure
	 * @return the fallback response
	 */
	ClientHttpResponse fallbackResponse(Throwable cause);
}
