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

package org.springframework.cloud.netflix.feign.encoding;

import feign.Client;
import feign.Feign;
import okhttp3.OkHttpClient;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Feign request compression.
 *
 * @author Jakub Narloch
 * @see FeignContentGzipEncodingInterceptor
 */
@Configuration
@EnableConfigurationProperties(FeignClientEncodingProperties.class)
@ConditionalOnClass(Feign.class)
@ConditionalOnBean(Client.class)
//The OK HTTP client uses "transparent" compression.
//If the content-encoding header is present it disable transparent compression
@ConditionalOnMissingBean(OkHttpClient.class)
@ConditionalOnProperty(value = "feign.compression.request.enabled", matchIfMissing = false)
@AutoConfigureAfter(FeignAutoConfiguration.class)
public class FeignContentGzipEncodingAutoConfiguration {

	@Bean
	public FeignContentGzipEncodingInterceptor feignContentGzipEncodingInterceptor(FeignClientEncodingProperties properties) {
		return new FeignContentGzipEncodingInterceptor(properties);
	}
}
