/*
 * Copyright 2018-2022 the original author or authors.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * We need to use a suite cause we need to first run all the Eureka Servers, then close
 * all of them. We can't run one EurekaServer, close it and then run another one, cause
 * Eureka is using static executor services that are shutdown when we close a context.
 * That means that when the new context starts we will fail cause the executor service is
 * already shutdown.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ EurekaClientTest.class, RestTemplateEurekaClientTest.class })
public class EurekaClientSuite {

}
