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

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
import feign.ribbon.LoadBalancingTarget;
import feign.slf4j.Slf4jLogger;

/**
 * @author Spencer Gibb
 */
@Data
@EqualsAndHashCode(callSuper = false)
class FeignClientFactoryBean implements FactoryBean<Object>, InitializingBean {

	private Class<?> type;

	private String name;

	private String url;

	@Autowired
	private Decoder decoder;

	@Autowired
	private Encoder encoder;

	@Autowired
	private Logger logger;

	@Autowired
	private Contract contract;

	@Autowired(required = false)
	private Logger.Level logLevel;

	@Autowired(required = false)
	private Retryer retryer;

	@Autowired(required = false)
	private ErrorDecoder errorDecoder;

	@Autowired(required = false)
	private Request.Options options;

	@Autowired(required = false)
	private Client ribbonClient;

	@Autowired(required = false)
	private List<RequestInterceptor> requestInterceptors;

	@Override
	public void afterPropertiesSet() throws Exception {
		if (StringUtils.hasText(this.name)) {
			Assert.state(!StringUtils.hasText(this.url),
					"Either value or url can be specified, but not both");
		}
	}

	protected Feign.Builder feign() {
		Feign.Builder builder = Feign.builder()
				// required values
				.logger(this.logger).encoder(this.encoder).decoder(this.decoder)
				.contract(this.contract);

		// optional values
		if (this.logLevel != null) {
			builder.logLevel(this.logLevel);
		}
		if (this.retryer != null) {
			builder.retryer(this.retryer);
		}
		if (this.errorDecoder != null) {
			builder.errorDecoder(this.errorDecoder);
		}
		if (this.options != null) {
			builder.options(this.options);
		}
		if (this.requestInterceptors != null) {
			builder.requestInterceptors(this.requestInterceptors);
		}

		return builder;
	}

	protected <T> T loadBalance(Feign.Builder builder, Class<T> type, String schemeName) {
		builder.logger(new Slf4jLogger(type)); // TODO: how to have choice here?
		if (this.ribbonClient != null) {
			return builder.client(this.ribbonClient).target(type, schemeName);
		}
		else {
			return builder.target(LoadBalancingTarget.create(type, schemeName));
		}
	}

	@Override
	public Object getObject() throws Exception {
		if (StringUtils.hasText(this.name) && !this.name.startsWith("http")) {
			this.name = "http://" + this.name;
		}
		if (StringUtils.hasText(this.name)) {
			return loadBalance(feign(), this.type, this.name);
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
