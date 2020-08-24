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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.assertj.core.api.Assertions.assertThat;

public class EurekaClientTest extends BaseCertTest {

	private static EurekaServerRunner server;

	private static EurekaClientRunner service;

	@BeforeClass
	public static void setupAll() {
		startEurekaServer();
		startService();
		waitForRegistration();
	}

	@AfterClass
	public static void tearDownAll() {
		stopService();
		stopEurekaServer();
	}

	private static void startEurekaServer() {
		server = new EurekaServerRunner(TestEurekaServer.class);
		server.enableTls();
		server.setKeyStore(serverCert, KEY_STORE_PASSWORD, "server", KEY_PASSWORD);
		server.setTrustStore(caCert, KEY_STORE_PASSWORD);

		server.start();
	}

	private static void stopEurekaServer() {
		server.stop();
	}

	private static void startService() {
		service = new EurekaClientRunner(TestApp.class, server, "testservice");
		enableTlsClient(service);
		service.start();
	}

	private static void stopService() {
		service.stop();
	}

	private static void waitForRegistration() {
		try (EurekaClientRunner client = createEurekaClient()) {
			enableTlsClient(client);
			client.start();
			client.waitServiceViaEureka(60);
		}
	}

	private static EurekaClientRunner createEurekaClient() {
		return new EurekaClientRunner(TestApp.class, server);
	}

	private static void enableTlsClient(EurekaClientRunner runner) {
		runner.enableTls();
		runner.setKeyStore(clientCert, KEY_STORE_PASSWORD, KEY_PASSWORD);
		runner.setTrustStore(caCert, KEY_STORE_PASSWORD);
	}

	/**
	 * Already proved this in waitForRegistration(). Keep this Test to express test
	 * purpose explicitly.
	 */
	@Test
	public void clientCertCanWork() {
	}

	@Test
	public void noCertCannotWork() {
		try (EurekaClientRunner client = createEurekaClient()) {
			client.disableTls();
			client.start();
			assertThat(client.foundServiceViaEureka()).isFalse();
		}
	}

	@Test
	public void wrongCertCannotWork() {
		try (EurekaClientRunner client = createEurekaClient()) {
			enableTlsClient(client);
			client.setKeyStore(wrongClientCert);
			client.start();
			assertThat(client.foundServiceViaEureka()).isFalse();
		}
	}

	@Test(expected = BeanCreationException.class)
	public void wrongPasswordCauseFailure() {
		EurekaClientRunner client = createEurekaClient();
		enableTlsClient(client);
		client.setKeyStore(clientCert, WRONG_PASSWORD, WRONG_PASSWORD);
		client.start();
	}

	@Test(expected = BeanCreationException.class)
	public void nonExistKeyStoreCauseFailure() {
		EurekaClientRunner client = createEurekaClient();
		enableTlsClient(client);
		client.setKeyStore(new File("nonExistFile"));
		client.start();
	}

	@Test
	public void wrongTrustStoreCannotWork() {
		try (EurekaClientRunner client = createEurekaClient()) {
			enableTlsClient(client);
			client.setTrustStore(wrongCaCert);
			client.start();
			assertThat(client.foundServiceViaEureka()).isFalse();
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableEurekaServer
	public static class TestEurekaServer {

	}

}
