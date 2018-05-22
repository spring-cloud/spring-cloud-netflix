/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.cloud.netflix.hystrix.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * @author Dave Syer
 * @author Daniel Lavoie
 *
 */
public class HystrixContractUtils {

	public static String simpleBody() {
		try {
			return StreamUtils.copyToString(new DefaultResourceLoader()
					.getResource("classpath:/stubs/simpleBody.json").getInputStream(),
					StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot read stub", e);
		}
	}

	public static void checkEvent(String event) {
		assertThat(event).isNotNull();
		assertThat(event).isEqualTo("message");
	}

	public static void checkOrigin(Map<String, Object> origin) {
		assertThat(origin.get("host")).isNotNull();
		assertThat(origin.get("port")).isNotNull();
		assertThat(origin.get("serviceId")).isEqualTo("application");
		// TODO: boot 2 changed application context id generation
		assertThat(origin.get("id")).asString().startsWith("application");
	}

	public static void checkData(Map<String, Object> data, String group, String name) {
		if (!data.get("type").equals("HystrixCommand")) {
			assertThat(data.get("type")).isEqualTo("HystrixThreadPool");
			return;
		}
		assertThat(data.get("type")).isEqualTo("HystrixCommand");
		if (!data.get("name").equals(name)) {
			return;
		}
		assertThat(data.get("name")).asString().isEqualTo(name);
		assertThat(data.get("group")).isNotNull();
		assertThat(data.get("group")).isEqualTo(group);
		assertThat(data.get("errorCount")).isEqualTo(0);
		assertThat(data.get("errorPercentage")).isEqualTo(0);
		assertThat(data.get("requestCount")).isInstanceOf(java.lang.Integer.class);
		assertThat(data.get("currentConcurrentExecutionCount"))
				.isInstanceOf(java.lang.Integer.class);
		assertThat(data.get("rollingCountFailure")).isEqualTo(0);
		assertThat(data.get("rollingCountSuccess")).isInstanceOf(java.lang.Integer.class);
		assertThat(data.get("rollingCountShortCircuited")).isEqualTo(0);
		assertThat(data.get("rollingCountFallbackSuccess")).isEqualTo(0);
		assertThat(data.get("isCircuitBreakerOpen")).isEqualTo(false);
	}

}
