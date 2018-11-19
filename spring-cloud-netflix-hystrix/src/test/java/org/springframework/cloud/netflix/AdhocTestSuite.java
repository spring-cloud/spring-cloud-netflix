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

/**
 * A test suite for probing weird ordering problems in the tests.
 *
 * @author Dave Syer
 */
@RunWith(Suite.class)
@SuiteClasses({
		// org.springframework.cloud.netflix.test.OkHttpClientConfigurationTests.class,
		// org.springframework.cloud.netflix.test.ApacheHttpClientConfigurationTests.class,
		// org.springframework.cloud.netflix.hystrix.HystrixCommandsTests.class,
		// org.springframework.cloud.netflix.hystrix.HystrixOnlyTests.class,
		// org.springframework.cloud.netflix.hystrix.security.HystrixSecurityTests.class,
		// org.springframework.cloud.netflix.hystrix.security.HystrixSecurityNoFeignTests.class,
		// org.springframework.cloud.netflix.hystrix.HystrixStreamEndpointTests.class,
		// org.springframework.cloud.netflix.hystrix.HystrixConfigurationTests.class,
		// org.springframework.cloud.netflix.resttemplate.RestTemplateRetryTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessorOverridesRetryTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonUtilsTests.class,
		// org.springframework.cloud.netflix.ribbon.test.RibbonClientDefaultConfigurationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientConfigurationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactoryTests.class,
		// org.springframework.cloud.netflix.ribbon.SpringRetryEnabledTests.class,
		// org.springframework.cloud.netflix.ribbon.PlainRibbonClientPreprocessorIntegrationTests.class,
		// org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClientTests.class,
		// org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpRequestTests.class,
		// org.springframework.cloud.netflix.ribbon.apache.RibbonApacheHttpResponseTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessorIntegrationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientsPreprocessorIntegrationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientsEagerInitializationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonInterceptorTests.class,
		// org.springframework.cloud.netflix.ribbon.support.ContextAwareRequestTests.class,
		// org.springframework.cloud.netflix.ribbon.support.RibbonCommandContextTest.class,
		// org.springframework.cloud.netflix.ribbon.support.RetryableStatusCodeExceptionTests.class,
		// org.springframework.cloud.netflix.ribbon.ZonePreferenceServerListFilterTests.class,
		// org.springframework.cloud.netflix.ribbon.DefaultServerIntrospectorDefaultTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessorPropertiesOverridesIntegrationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactoryTests.class,
		// org.springframework.cloud.netflix.ribbon.SpringClientFactoryTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonAutoConfigurationIntegrationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClientTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessorOverridesIntegrationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonApplicationContextInitializerTests.class,
		// org.springframework.cloud.netflix.ribbon.okhttp.SpringRetryDisableOkHttpClientTests.class,
		// org.springframework.cloud.netflix.ribbon.okhttp.OkHttpRibbonResponseTests.class,
		// org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClientTests.class,
		// org.springframework.cloud.netflix.ribbon.okhttp.OkHttpRibbonRequestTests.class,
		// org.springframework.cloud.netflix.ribbon.okhttp.SpringRetryEnabledOkHttpClientTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonClientConfigurationIntegrationTests.class,
		// org.springframework.cloud.netflix.ribbon.RibbonDisabledTests.class,
		// org.springframework.cloud.netflix.ribbon.DefaultServerIntrospectorTests.class,
		// org.springframework.cloud.netflix.ribbon.SpringRetryDisabledTests.class,
		// org.springframework.cloud.netflix.feign.beans.FeignClientTests.class,
		// org.springframework.cloud.netflix.feign.FeignClientsRegistrarTests.class,
		// org.springframework.cloud.netflix.feign.encoding.FeignAcceptEncodingTests.class,
		// org.springframework.cloud.netflix.feign.encoding.FeignContentEncodingTests.class,
		// org.springframework.cloud.netflix.feign.FeignLoggerFactoryTests.class,
		// org.springframework.cloud.netflix.feign.FeignCompressionTests.class,
		// org.springframework.cloud.netflix.feign.EnableFeignClientsTests.class,
		// org.springframework.cloud.netflix.feign.SpringDecoderTests.class,
		// org.springframework.cloud.netflix.feign.FeignClientUsingPropertiesTests.class,
		// org.springframework.cloud.netflix.feign.FeignHttpClientUrlTests.class,
		// org.springframework.cloud.netflix.feign.invalid.FeignClientValidationTests.class,
		// org.springframework.cloud.netflix.feign.support.FeignHttpClientPropertiesTests.class,
		// org.springframework.cloud.netflix.feign.support.SpringMvcContractTests.class,
		// org.springframework.cloud.netflix.feign.support.SpringEncoderTests.class,
		// org.springframework.cloud.netflix.feign.FeignClientOverrideDefaultsTests.class,
		// org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClientOverrideTests.class,
		// org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientPathTests.class,
		// org.springframework.cloud.netflix.feign.ribbon.RetryableFeignLoadBalancerTests.class,
		// org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientRetryTests.class,
		// org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancerTests.class,
		// org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientTests.class,
		// org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactoryTests.class,
		// org.springframework.cloud.netflix.feign.valid.scanning.FeignClientEnvVarTests.class,
		// org.springframework.cloud.netflix.feign.valid.scanning.FeignClientScanningTests.class,
		// org.springframework.cloud.netflix.feign.valid.FeignOkHttpTests.class,
		// org.springframework.cloud.netflix.feign.valid.FeignClientValidationTests.class,
		// org.springframework.cloud.netflix.feign.valid.FeignClientTests.class,
		// org.springframework.cloud.netflix.feign.valid.FeignHttpClientTests.class,
		// org.springframework.cloud.netflix.feign.valid.FeignClientNotPrimaryTests.class,
		// org.springframework.cloud.netflix.feign.FeignClientFactoryTests.class,

})
@Ignore
public class AdhocTestSuite {

}
