/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.netflix.feign;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import feign.Logger;
import feign.slf4j.Slf4jLogger;

/**
 * @author Venil Noronha
 */
public class FeignLoggerFactoryTests {

	@Test
	public void testDefaultLogger() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration1.class);
		FeignLoggerFactory loggerFactory = context.getBean(FeignLoggerFactory.class);
		assertNotNull(loggerFactory);
		Logger logger = loggerFactory.create(Object.class);
		assertNotNull(logger);
		assertTrue(logger instanceof Slf4jLogger);
		context.close();
	}

	@Configuration
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration1 {

	}

	@Test
	public void testCustomLogger() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration2.class);
		FeignLoggerFactory loggerFactory = context.getBean(FeignLoggerFactory.class);
		assertNotNull(loggerFactory);
		Logger logger = loggerFactory.create(Object.class);
		assertNotNull(logger);
		assertTrue(logger instanceof LoggerImpl1);
		context.close();
	}

	@Configuration
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration2 {

		@Bean
		public Logger logger() {
			return new LoggerImpl1();
		}

	}

	static class LoggerImpl1 extends Logger {

		@Override
		protected void log(String arg0, String arg1, Object... arg2) {
			// noop
		}

	}

	@Test
	public void testCustomLoggerFactory() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SampleConfiguration3.class);
		FeignLoggerFactory loggerFactory = context.getBean(FeignLoggerFactory.class);
		assertNotNull(loggerFactory);
		assertTrue(loggerFactory instanceof LoggerFactoryImpl);
		Logger logger = loggerFactory.create(Object.class);
		assertNotNull(logger);
		assertTrue(logger instanceof LoggerImpl2);
		context.close();
	}

	@Configuration
	@Import(FeignClientsConfiguration.class)
	protected static class SampleConfiguration3 {

		@Bean
		public FeignLoggerFactory feignLoggerFactory() {
			return new LoggerFactoryImpl();
		}

	}

	static class LoggerFactoryImpl implements FeignLoggerFactory {

		@Override
		public Logger create(Class<?> type) {
			return new LoggerImpl2();
		}

	}

	static class LoggerImpl2 extends Logger {

		@Override
		protected void log(String arg0, String arg1, Object... arg2) {
			// noop
		}

	}

}
