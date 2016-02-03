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

package org.springframework.cloud.netflix.feign.encoding.app.client;

import java.util.List;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.encoding.app.domain.Invoice;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Simple Feign client for retrieving the invoice list.
 *
 * @author Jakub Narloch
 */
@FeignClient("local")
public interface InvoiceClient {

	@RequestMapping(value = "invoices", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> getInvoices();

	@RequestMapping(value = "invoices", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> saveInvoices(List<Invoice> invoices);
}
