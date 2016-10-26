/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.cloud.netflix;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.cloud.netflix.feign.encoding.FeignAcceptEncodingTests;
import org.springframework.cloud.netflix.metrics.servo.ServoMetricReaderTests;
import org.springframework.cloud.netflix.ribbon.RibbonInterceptorTests;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClientTests;
import org.springframework.cloud.netflix.zuul.ZuulProxyConfigurationTests;

/**
 * A test suite for probing weird ordering problems in the tests.
 *
 * @author Dave Syer
 */
@RunWith(Suite.class)
@SuiteClasses({ RibbonLoadBalancerClientTests.class, RibbonInterceptorTests.class, FeignAcceptEncodingTests.class,
	ServoMetricReaderTests.class, ZuulProxyConfigurationTests.class })
@Ignore
public class AdhocTestSuite {

}
