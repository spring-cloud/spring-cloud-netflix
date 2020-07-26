/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContextBuilder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Eureka client TLS properties.
 */
@ConfigurationProperties(TlsProperties.PREFIX)
public class TlsProperties {

	/**
	 * Prefix for Eureka client TLS properties.
	 */
	public static final String PREFIX = "eureka.client.tls";

	private static final String DEFAULT_STORE_TYPE = "PKCS12";

	private static final Map<String, String> EXTENSION_STORE_TYPES = extTypes();

	private boolean enabled;

	private Resource keyStore;

	private String keyStoreType;

	private String keyStorePassword = "";

	private String keyPassword = "";

	private Resource trustStore;

	private String trustStoreType;

	private String trustStorePassword = "";

	private static Map<String, String> extTypes() {
		Map<String, String> result = new HashMap<>();

		result.put("p12", "PKCS12");
		result.put("pfx", "PKCS12");
		result.put("jks", "JKS");

		return Collections.unmodifiableMap(result);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Resource getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(Resource keyStore) {
		this.keyStore = keyStore;
	}

	public String getKeyStoreType() {
		return keyStoreType;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public char[] keyStorePassword() {
		return keyStorePassword.toCharArray();
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public char[] keyPassword() {
		return keyPassword.toCharArray();
	}

	public Resource getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(Resource trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStoreType() {
		return trustStoreType;
	}

	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public char[] trustStorePassword() {
		return trustStorePassword.toCharArray();
	}

	@PostConstruct
	public void postConstruct() {
		if (keyStore != null && keyStoreType == null) {
			keyStoreType = storeTypeOf(keyStore);
		}
		if (trustStore != null && trustStoreType == null) {
			trustStoreType = storeTypeOf(trustStore);
		}
	}

	private String storeTypeOf(Resource resource) {
		String extension = fileExtensionOf(resource);
		String type = EXTENSION_STORE_TYPES.get(extension);

		return (type == null) ? DEFAULT_STORE_TYPE : type;
	}

	private String fileExtensionOf(Resource resource) {
		String name = resource.getFilename();
		int index = name.lastIndexOf('.');

		return index < 0 ? "" : name.substring(index + 1).toLowerCase();
	}

	public SSLContext createSSLContext() throws GeneralSecurityException, IOException {
		SSLContextBuilder builder = new SSLContextBuilder();
		char[] keyPassword = keyPassword();
		KeyStore keyStore = createKeyStore();

		try {
			builder.loadKeyMaterial(keyStore, keyPassword);
		}
		catch (UnrecoverableKeyException e) {
			if (keyPassword.length == 0) {
				// Retry if empty password, see
				// https://rt.openssl.org/Ticket/Display.html?id=1497&user=guest&pass=guest
				builder.loadKeyMaterial(keyStore, new char[] { '\0' });
			}
			else {
				throw e;
			}
		}

		KeyStore trust = createTrustStore();
		if (trust != null) {
			builder.loadTrustMaterial(trust, null);
		}

		return builder.build();
	}

	private KeyStore createKeyStore() throws GeneralSecurityException, IOException {
		if (keyStore == null) {
			throw new KeyStoreException("Keystore not specified.");
		}
		if (!keyStore.exists()) {
			throw new KeyStoreException("Keystore not exists: " + keyStore);
		}

		KeyStore result = KeyStore.getInstance(keyStoreType);
		char[] keyStorePassword = keyStorePassword();

		try {
			loadKeyStore(result, keyStore, keyStorePassword);
		}
		catch (IOException e) {
			// Retry if empty password, see
			// https://rt.openssl.org/Ticket/Display.html?id=1497&user=guest&pass=guest
			if (keyStorePassword.length == 0) {
				loadKeyStore(result, keyStore, new char[] { '\0' });
			}
			else {
				throw e;
			}
		}

		return result;
	}

	private static void loadKeyStore(KeyStore keyStore, Resource keyStoreResource,
			char[] keyStorePassword) throws IOException, GeneralSecurityException {
		try (InputStream inputStream = keyStoreResource.getInputStream()) {
			keyStore.load(inputStream, keyStorePassword);
		}
	}

	private KeyStore createTrustStore() throws GeneralSecurityException, IOException {
		if (trustStore == null) {
			return null;
		}
		if (!trustStore.exists()) {
			throw new KeyStoreException("KeyStore not exists: " + trustStore);
		}

		KeyStore result = KeyStore.getInstance(trustStoreType);
		try (InputStream input = trustStore.getInputStream()) {
			result.load(input, trustStorePassword());
		}
		return result;
	}

}
