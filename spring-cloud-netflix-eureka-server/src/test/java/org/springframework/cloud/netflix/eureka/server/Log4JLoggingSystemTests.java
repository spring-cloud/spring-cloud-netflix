/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.log4j.Log4JLoggingSystem;
import org.springframework.boot.test.OutputCapture;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.netflix.blitz4j.LoggingConfiguration;

/**
 * Tests for {@link Log4JLoggingSystem}.
 *
 * @author Phillip Webb
 */
public class Log4JLoggingSystemTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private final Log4JLoggingSystem loggingSystem = new Log4JLoggingSystem(getClass()
			.getClassLoader());

	private Logger logger;

	@Before
	public void setup() {
		System.setProperty("log4j.configuration", getPackagedConfigFile("log4j.properties"));
		logger = Logger.getLogger(getClass());
		LoggingConfiguration.getInstance().configure();
	}

	@Test
	@Ignore("gh-48")
	public void setLevel() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.cloud", LogLevel.DEBUG);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(this.output.toString(), "Hello"),
				equalTo(1));
	}


	private final String getPackagedConfigFile(String fileName) {
		try {
			return new ClassPathResource(fileName, Log4JLoggingSystem.class).getURL().toString();
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot create URL", e);
		}
	}
}
