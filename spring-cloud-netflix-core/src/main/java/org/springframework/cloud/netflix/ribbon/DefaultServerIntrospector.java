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

package org.springframework.cloud.netflix.ribbon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.netflix.loadbalancer.Server;

/**
 * @author Spencer Gibb
 */
public class DefaultServerIntrospector implements ServerIntrospector {
	private static final List<Integer> SECURE_PORTS = Arrays.asList(443, 8443);

	@Override
	public boolean isSecure(Server server) {
		return SECURE_PORTS.contains(server.getPort());
	}

	@Override
	public Map<String, String> getMetadata(Server server) {
		return Collections.emptyMap();
	}
}
