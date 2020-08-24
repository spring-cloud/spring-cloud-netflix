/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import java.io.File;

public class EurekaServerRunner extends AppRunner {

	public EurekaServerRunner(Class<?> appClass) {
		super(appClass);

		property("eureka.client.registerWithEureka", "false");
		property("eureka.client.fetchRegistry", "false");
		property("eureka.server.waitTimeInMsWhenSyncEmpty", "0");
		property("eureka.client.refresh.enable", "true");
	}

	public void enableTls() {
		property("server.ssl.enabled", "true");
		property("server.ssl.client-auth", "need");
	}

	public void setKeyStore(File keyStore, String keyStorePassword, String key,
			String keyPassword) {
		property("server.ssl.key-store", pathOf(keyStore));
		property("server.ssl.key-store-type", "PKCS12");
		property("server.ssl.key-store-password", keyStorePassword);
		property("server.ssl.key-alias", key);
		property("server.ssl.key-password", keyPassword);
	}

	public void setTrustStore(File trustStore, String password) {
		property("server.ssl.trust-store", pathOf(trustStore));
		property("server.ssl.trust-store-type", "PKCS12");
		property("server.ssl.trust-store-password", password);
	}

	private String pathOf(File file) {
		return String.format("file:%s", file.getAbsolutePath());
	}

}
