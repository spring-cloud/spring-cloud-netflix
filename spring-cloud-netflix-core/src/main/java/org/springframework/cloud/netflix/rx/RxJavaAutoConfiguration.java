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

package org.springframework.cloud.netflix.rx;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import rx.Observable;
import rx.Single;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(Observable.class)
public class RxJavaAutoConfiguration {

	@Configuration
	@ConditionalOnClass({ AsyncHandlerMethodReturnValueHandler.class, WebMvcConfigurerAdapter.class })
	protected static class RxJavaReturnValueHandlerConfig {
		@Bean
		public SingleReturnValueHandler singleReturnValueHandler() {
			return new SingleReturnValueHandler();
		}

		@Bean
		public WebMvcConfigurerAdapter observableMVCConfiguration(final SingleReturnValueHandler singleReturnValueHandler) {
			return new WebMvcConfigurerAdapter() {
				@Override
				public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
					returnValueHandlers.add(singleReturnValueHandler);
				}
			};
		}

		@Bean
		public HasFeatures rxFeature() {
			return HasFeatures.namedFeatures("MVC Observable", Observable.class, "MVC Single", Single.class);
		}
	}
}
