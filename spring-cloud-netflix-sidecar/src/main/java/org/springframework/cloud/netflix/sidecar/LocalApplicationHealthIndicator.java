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

package org.springframework.cloud.netflix.sidecar;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.client.RestTemplate;

/**
 * @author Spencer Gibb
 */
public class LocalApplicationHealthIndicator extends AbstractHealthIndicator {

	@Autowired
	private SidecarProperties properties;

	@SuppressWarnings("unchecked")
	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		URI uri = this.properties.getHealthUri();
		if (uri == null) {
			builder.up();
			return;
		}
		Map<String, Object> map = new RestTemplate().getForObject(uri, Map.class);
		Object status = map.get("status");
		if (status != null && status instanceof String) {
			builder.status(status.toString());
		}
		else if (status != null && status instanceof Map) {
			Map<String, Object> statusMap = (Map<String, Object>) status;
			Object code = statusMap.get("code");
			if (code != null) {
				builder.status(code.toString());
			}
			else {
				getWarning(builder);
			}
		}
		else {
			getWarning(builder);
		}
	}

	private Health.Builder getWarning(Health.Builder builder) {
		return builder.unknown().withDetail("warning", "no status field in response");
	}

}
