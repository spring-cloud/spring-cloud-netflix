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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
		if (StringUtils.hasText(this.name)) {
			Assert.state(!StringUtils.hasText(this.url),
					"Either value or url can be specified, but not both");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	protected Feign.Builder feign() {
		// @formatter:off
		Feign.Builder builder = Feign.builder()
				// required values
				.logger(get(Logger.class))
				.encoder(get(Encoder.class))
				.decoder(get(Decoder.class))
				.contract(get(Contract.class));
		// @formatter:on

		// optional values
		Logger.Level level = getOptional(Logger.Level.class);
		if (level != null) {
			builder.logLevel(level);
		}
		Retryer retryer = getOptional(Retryer.class);
		if (retryer != null) {
			builder.retryer(retryer);
		}
		ErrorDecoder errorDecoder = getOptional(ErrorDecoder.class);
		if (errorDecoder != null) {
			builder.errorDecoder(errorDecoder);
		}
		Request.Options options = getOptional(Request.Options.class);
		if (options != null) {
			builder.options(options);
		}
		Map<String, RequestInterceptor> requestInterceptors = this.context.getBeansOfType(RequestInterceptor.class);
		if (requestInterceptors != null) {
			builder.requestInterceptors(requestInterceptors.values());
		}

		return builder;
	}

	protected <T> T get(Class<T> type) {
		return this.context.getBean(type);
	}

	protected <T> T getOptional(Class<T> type) {
		try {
			return this.context.getBean(type);
		} catch (NoSuchBeanDefinitionException e) {
			//ignore
		}
		return null;
	}

	protected <T> T loadBalance(Feign.Builder builder, Class<T> type, String schemeName) {
		builder.logger(new Slf4jLogger(type)); // TODO: how to have choice here?
		Client client = getOptional(Client.class);
		if (client != null) {
			return builder.client(client).target(type, schemeName);
		}

		throw new IllegalStateException("No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-ribbon?");
	}

	@Override
	public Object getObject() throws Exception {
		if (StringUtils.hasText(this.name) && !this.name.startsWith("http")) {
			this.name = "http://" + this.name;
		}
		if (StringUtils.hasText(this.name)) {
			return loadBalance(feign(), this.type, this.name);
		}
		if (StringUtils.hasText(this.url) && !this.url.startsWith("http")) {
			this.url = "http://" + this.url;
		}
		return feign().target(this.type, this.url);
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
