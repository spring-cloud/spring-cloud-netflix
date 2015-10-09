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

package org.springframework.cloud.netflix.eureka;

import com.netflix.appinfo.InstanceInfo;

/**
 * @author Spencer Gibb
 */
class MutableInstanceInfo extends InstanceInfo {

	private Integer port;

	public MutableInstanceInfo(InstanceInfo ii) {
		super(ii);
	}

	@Override
	public int getPort() {
		if (this.port != null) {
			return this.port;
		}
		return super.getPort();
	}

	public void setPort(int port) {
		this.port = port;
	}

}
