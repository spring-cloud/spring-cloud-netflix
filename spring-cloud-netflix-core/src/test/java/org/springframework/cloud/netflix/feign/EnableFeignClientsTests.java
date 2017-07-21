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

package org.springframework.cloud.netflix.feign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringEncoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.slf4j.Slf4jLogger;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EnableFeignClientsTests.PlainConfiguration.class)
@DirtiesContext
public class EnableFeignClientsTests {

	@Autowired
	private FeignContext feignContext;

	@Test
	public void decoderDefaultCorrect() {
		ResponseEntityDecoder.class
				.cast(this.feignContext.getInstance("foo", Decoder.class));
	}

	@Test
	public void encoderDefaultCorrect() {
		SpringEncoder.class.cast(this.feignContext.getInstance("foo", Encoder.class));
	}

	@Test
	public void loggerDefaultCorrect() {
		Slf4jLogger.class.cast(this.feignContext.getInstance("foo", Logger.class));
	}

	@Test
	public void contractDefaultCorrect() {
		SpringMvcContract.class
				.cast(this.feignContext.getInstance("foo", Contract.class));
	}

	@Test
	public void builderDefaultCorrect() {
		Feign.Builder.class
				.cast(this.feignContext.getInstance("foo", Feign.Builder.class));
	}

	@Configuration
	@Import({ PropertyPlaceholderAutoConfiguration.class, ArchaiusAutoConfiguration.class,
			FeignAutoConfiguration.class })
	protected static class PlainConfiguration {
	}

}
