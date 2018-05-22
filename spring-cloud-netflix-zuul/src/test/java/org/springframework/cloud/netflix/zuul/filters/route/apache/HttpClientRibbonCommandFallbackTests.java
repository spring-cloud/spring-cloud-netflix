/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul.filters.route.apache;

import com.netflix.zuul.context.RequestContext;

import org.junit.Before;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.filters.route.support.RibbonCommandFallbackTests;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RibbonCommandFallbackTests.TestConfig.class, webEnvironment = RANDOM_PORT, properties = {
		"zuul.routes.simple: /simple/**", "zuul.routes.another: /another/twolevel/**",
		"ribbon.ReadTimeout: 1" })
@DirtiesContext
public class HttpClientRibbonCommandFallbackTests extends RibbonCommandFallbackTests {

	@Before
	public void init() {
		RequestContext.getCurrentContext().clear();
	}

}
