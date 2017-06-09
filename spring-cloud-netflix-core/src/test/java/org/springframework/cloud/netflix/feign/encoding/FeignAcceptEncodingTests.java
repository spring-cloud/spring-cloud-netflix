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

package org.springframework.cloud.netflix.feign.encoding;

import java.util.Collections;
import java.util.List;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.encoding.app.client.InvoiceClient;
import org.springframework.cloud.netflix.feign.encoding.app.domain.Invoice;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the response compression.
 *
 * @author Jakub Narloch
 */
@SpringBootTest(classes = FeignAcceptEncodingTests.Application.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"feign.compression.response.enabled=true" })
@RunWith(SpringRunner.class)
@DirtiesContext
public class FeignAcceptEncodingTests {

	@Autowired
	private InvoiceClient invoiceClient;

	@Test
	public void compressedResponse() {

		// when
		final ResponseEntity<List<Invoice>> invoices = this.invoiceClient.getInvoices();

		// then
		assertNotNull(invoices);
		assertEquals(HttpStatus.OK, invoices.getStatusCode());
		assertNotNull(invoices.getBody());
		assertEquals(100, invoices.getBody().size());

	}

	@EnableFeignClients(clients = InvoiceClient.class)
	@RibbonClient(name = "local", configuration = LocalRibbonClientConfiguration.class)
	@SpringBootApplication(scanBasePackages = "org.springframework.cloud.netflix.feign.encoding.app")
	public static class Application {
	}

	@Configuration
	static class LocalRibbonClientConfiguration {

		@Value("${local.server.port}")
		private int port = 0;

		@Bean
		public ILoadBalancer ribbonLoadBalancer() {
			BaseLoadBalancer balancer = new BaseLoadBalancer();
			balancer.setServersList(
					Collections.singletonList(new Server("localhost", this.port)));
			return balancer;
		}
	}
}