/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.metrics.servo;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ExportMetricReader;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 */
@SpringBootTest(properties = {"spring.metrics.servo.enabled:true"})
@RunWith(SpringJUnit4ClassRunner.class)
public class ServoMetricsAutoConfigurationTests {

	@Autowired(required = false)
	private ServoMetricNaming naming;

	@Autowired(required = false)
	@ExportMetricReader
	private MetricReader reader;

	@Test
	public void test() {
		assertNotNull(this.naming);
		assertNotNull(this.reader);
	}

	@EnableAutoConfiguration
	@Configuration
	protected static class TestConfiguration {

	}

}
