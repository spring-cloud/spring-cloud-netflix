/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.feign.encoding.app.resource;

import org.springframework.cloud.netflix.feign.encoding.app.domain.Invoice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An sample REST controller, that potentially returns large response - used for testing.
 *
 * @author Jakub Narloch
 */
@RestController
public class InvoiceResource {

	@RequestMapping(value = "invoices", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Invoice>> getInvoices() {

		return ResponseEntity.ok(createInvoiceList(100));
	}

	@RequestMapping(value = "invoices", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> saveInvoices(@RequestBody List<Invoice> invoices) {

		return ResponseEntity.ok(invoices);
	}

	private List<Invoice> createInvoiceList(int count) {
		final List<Invoice> invoices = new ArrayList<>();
		for (int ind = 0; ind < count; ind++) {
			final Invoice invoice = new Invoice();
			invoice.setTitle("Invoice " + (ind + 1));
			invoice.setAmount(new BigDecimal(String.format(Locale.US, "%.2f", Math.random() * 1000)));
			invoices.add(invoice);
		}
		return invoices;
	}
}
