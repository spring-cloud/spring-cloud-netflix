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

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author Spencer Gibb
 */
@Data
@EqualsAndHashCode(callSuper = false)
class FeignClientFactoryBean extends FeignConfiguration implements FactoryBean<Object> {

	private boolean loadbalance;

	private Class<?> type;

	private String schemeName;

	@Override
	public Object getObject() throws Exception {
		if (!this.schemeName.startsWith("http")) {
			this.schemeName = "http://" + this.schemeName;
		}
		if (this.loadbalance) {
			return loadBalance(this.type, this.schemeName);
		}
		return feign().target(this.type, this.schemeName);
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
