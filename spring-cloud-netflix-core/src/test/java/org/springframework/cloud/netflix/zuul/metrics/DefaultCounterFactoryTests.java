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

package org.springframework.cloud.netflix.zuul.metrics;

import org.springframework.boot.actuate.metrics.CounterService;

import com.netflix.zuul.monitoring.CounterFactory;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultCounterFactoryTests {

	private static final String NAME = "my-super-metric-name";
	private final CounterService counterService = mock(CounterService.class);
	private final CounterFactory factory = new DefaultCounterFactory(counterService);

	@Test
	public void shouldIncrement() throws Exception {
		factory.increment(NAME);

		verify(counterService).increment(NAME);
	}
}