/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.feign;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.slf4j.Slf4jLogger;

/**
 * @author Spencer Gibb
 */
@Data
@EqualsAndHashCode(callSuper = false)
class FeignClientFactoryBean implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

	private Class<?> type;

	private String name;

	private String url;

	private ApplicationContext context;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.name, "Name must be set");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	protected Feign.Builder feign(FeignClientFactory factory) {
		Logger logger = getOptional(factory, Logger.class);

		if (logger == null) {
			logger = new Slf4jLogger(this.type);
		}

		// @formatter:off
		Feign.Builder builder = Feign.builder()
				// required values
				.logger(logger)
				.encoder(get(factory, Encoder.class))
				.decoder(get(factory, Decoder.class))
				.contract(get(factory, Contract.class));
		// @formatter:on

		// optional values
		Logger.Level level = getOptional(factory, Logger.Level.class);
		if (level != null) {
			builder.logLevel(level);
		}
		Retryer retryer = getOptional(factory, Retryer.class);
		if (retryer != null) {
			builder.retryer(retryer);
		}
		ErrorDecoder errorDecoder = getOptional(factory, ErrorDecoder.class);
		if (errorDecoder != null) {
			builder.errorDecoder(errorDecoder);
		}
		Request.Options options = getOptional(factory, Request.Options.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, RequestInterceptor> requestInterceptors = this.context.getBeansOfType(RequestInterceptor.class);
		if (requestInterceptors != null) {
			builder.requestInterceptors(requestInterceptors.values());
		}

		return builder;
	}

	protected <T> T get(FeignClientFactory factory, Class<T> type) {
		T instance = factory.getInstance(this.name, type);
		if (instance == null) {
			throw new IllegalStateException("No bean found of type " + type + " for " + this.name);
		}
		return instance;
	}

	protected <T> T getOptional(FeignClientFactory factory, Class<T> type) {
		return factory.getInstance(this.name, type);
	}

	protected <T> T loadBalance(Feign.Builder builder, FeignClientFactory factory, Class<T> type, String url) {
		Client client = getOptional(factory, Client.class);
		if (client != null) {
			return builder.client(client).target(type, url);
		}

		throw new IllegalStateException("No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-ribbon?");
	}

	@Override
	public Object getObject() throws Exception {
		FeignClientFactory factory = context.getBean(FeignClientFactory.class);

		if (!StringUtils.hasText(this.url)) {
			String url;
			if (!this.name.startsWith("http")) {
				url = "http://" + this.name;
			} else {
				url = this.name;
			}
			return loadBalance(feign(factory), factory, this.type, url);
		}
		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}
		return feign(factory).target(this.type, this.url);
	}

	@Override
	public Class<?> getObjectType() {
		return this.type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
