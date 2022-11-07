/*
 * Copyright 2018-2022 the original author or authors.
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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;

import static org.assertj.core.api.Assertions.assertThat;

abstract class BaseCertTest {

	private static final Log log = LogFactory.getLog(BaseCertTest.class);

	protected static final String KEY_STORE_PASSWORD = "test-key-store-password";

	protected static final String KEY_PASSWORD = "test-key-password";

	protected static final String WRONG_PASSWORD = "test-wrong-password";

	protected static File caCert;

	protected static File wrongCaCert;

	protected static File serverCert;

	protected static File clientCert;

	protected static File wrongClientCert;

	protected BaseCertTest() {
	}

	@SuppressWarnings("rawtypes")
	static EurekaServerRunner startEurekaServer(Class config) {
		EurekaServerRunner server = new EurekaServerRunner(config);
		server.enableTls();
		server.setKeyStore(serverCert, KEY_STORE_PASSWORD, "server", KEY_PASSWORD);
		server.setTrustStore(caCert, KEY_STORE_PASSWORD);

		server.start();
		return server;
	}

	static void stopEurekaServer(EurekaServerRunner server) {
		server.stop();
	}

	@SuppressWarnings("rawtypes")
	static EurekaClientRunner startService(EurekaServerRunner server, Class config) {
		EurekaClientRunner service = new EurekaClientRunner(config, server, "testservice");
		enableTlsClient(service);
		service.start();
		return service;
	}

	static void stopService(EurekaClientRunner service) {
		service.stop();
	}

	static void enableTlsClient(EurekaClientRunner runner) {
		runner.enableTls();
		runner.setKeyStore(clientCert, KEY_STORE_PASSWORD, KEY_PASSWORD);
		runner.setTrustStore(caCert, KEY_STORE_PASSWORD);
	}

	static void waitForRegistration(Supplier<EurekaClientRunner> clientSupplier) {
		try (EurekaClientRunner client = clientSupplier.get()) {
			enableTlsClient(client);
			client.start();
			client.waitServiceViaEureka(60);
		}
	}

	@BeforeAll
	static void createCertificates() throws Exception {
		KeyTool tool = new KeyTool();

		KeyAndCert ca = tool.createCA("MyCA");
		KeyAndCert server = ca.sign("server");
		KeyAndCert client = ca.sign("client");

		caCert = saveCert(ca);
		serverCert = saveKeyAndCert(server);
		clientCert = saveKeyAndCert(client);

		KeyAndCert wrongCa = tool.createCA("WrongCA");
		KeyAndCert wrongClient = wrongCa.sign("client");

		wrongCaCert = saveCert(wrongCa);
		wrongClientCert = saveKeyAndCert(wrongClient);
	}

	@AfterAll
	static void afterClass() {
		log.info("Tests finished!");
	}

	abstract EurekaClientRunner createEurekaClient();

	/**
	 * Already proved this in waitForRegistration(). Keep this Test to express test
	 * purpose explicitly.
	 */
	@Test
	void clientCertCanWork() {
	}

	@Test
	void noCertCannotWork() {
		try (EurekaClientRunner client = createEurekaClient()) {
			client.disableTls();
			client.start();
			assertThat(client.foundServiceViaEureka()).isFalse();
		}
	}

	@Test
	void wrongCertCannotWork() {
		try (EurekaClientRunner client = createEurekaClient()) {
			enableTlsClient(client);
			client.setKeyStore(wrongClientCert);
			client.start();
			assertThat(client.foundServiceViaEureka()).isFalse();
		}
	}

	@Test
	void wrongPasswordCauseFailure() {
		EurekaClientRunner client = createEurekaClient();
		enableTlsClient(client);
		client.setKeyStore(clientCert, WRONG_PASSWORD, WRONG_PASSWORD);
		Assertions.assertThrows(BeanCreationException.class, client::start);
	}

	@Test
	void nonExistKeyStoreCauseFailure() {
		EurekaClientRunner client = createEurekaClient();
		enableTlsClient(client);
		client.setKeyStore(new File("nonExistFile"));
		Assertions.assertThrows(BeanCreationException.class, client::start);
	}

	@Test
	void wrongTrustStoreCannotWork() {
		try (EurekaClientRunner client = createEurekaClient()) {
			enableTlsClient(client);
			client.setTrustStore(wrongCaCert);
			client.start();
			assertThat(client.foundServiceViaEureka()).isFalse();
		}
	}

	private static File saveKeyAndCert(KeyAndCert keyCert) throws Exception {
		return saveKeyStore(keyCert.subject(), () -> keyCert.storeKeyAndCert(KEY_PASSWORD));
	}

	private static File saveCert(KeyAndCert keyCert) throws Exception {
		return saveKeyStore(keyCert.subject(), keyCert::storeCert);
	}

	private static File saveKeyStore(String prefix, KeyStoreSupplier func) throws Exception {
		File result = File.createTempFile(prefix, ".p12");
		result.deleteOnExit();

		try (OutputStream output = new FileOutputStream(result)) {
			KeyStore store = func.createKeyStore();
			store.store(output, KEY_STORE_PASSWORD.toCharArray());
		}
		return result;
	}

	interface KeyStoreSupplier {

		KeyStore createKeyStore() throws Exception;

	}

}
