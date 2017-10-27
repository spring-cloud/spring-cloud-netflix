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
 *
 */

package org.springframework.cloud.netflix.zuul.test;

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
org.springframework.cloud.netflix.zuul.ContextPathZuulProxyApplicationTests.class,
		org.springframework.cloud.netflix.zuul.filters.CompositeRouteLocatorTests.class,
		org.springframework.cloud.netflix.zuul.filters.CustomHostRoutingFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocatorTests.class,
		org.springframework.cloud.netflix.zuul.filters.discovery.PatternServiceRouteMapperIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.discovery.PatternServiceRouteMapperTests.class,
		org.springframework.cloud.netflix.zuul.filters.post.LocationRewriteFilterIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.post.LocationRewriteFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilterIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.post.SendResponseFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.pre.FormBodyWrapperFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelperTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFactoryTest.class,
		org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFallbackTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonRetryIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.EagerLoadOfZuulConfigurationTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.LazyLoadOfZuulConfigurationTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommandFactoryTest.class,
		org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommandFallbackTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommandIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonRetryIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.restclient.RestClientRibbonCommandFallbackTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.restclient.RestClientRibbonCommandIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommandTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.SendForwardFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilterTests.class,
		org.springframework.cloud.netflix.zuul.filters.route.support.RibbonCommandCauseFallbackPropagationTest.class,
		org.springframework.cloud.netflix.zuul.filters.route.support.RibbonCommandHystrixThreadPoolKeyTests.class,
		org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocatorTests.class,
		org.springframework.cloud.netflix.zuul.filters.ZuulPropertiesTests.class,
		org.springframework.cloud.netflix.zuul.FiltersEndpointTests.class,
		org.springframework.cloud.netflix.zuul.FormZuulProxyApplicationTests.class,
		org.springframework.cloud.netflix.zuul.FormZuulServletProxyApplicationTests.class,
		org.springframework.cloud.netflix.zuul.metrics.DefaultCounterFactoryTests.class,
		org.springframework.cloud.netflix.zuul.metrics.ZuulEmptyMetricsApplicationTests.class,
		org.springframework.cloud.netflix.zuul.metrics.ZuulMetricsApplicationTests.class,
		org.springframework.cloud.netflix.zuul.RetryableZuulProxyApplicationTests.class,
		org.springframework.cloud.netflix.zuul.RoutesEndpointDetailsTests.class,
		org.springframework.cloud.netflix.zuul.RoutesEndpointIntegrationTests.class,
		org.springframework.cloud.netflix.zuul.RoutesEndpointTests.class,
		org.springframework.cloud.netflix.zuul.ServletPathZuulProxyApplicationTests.class,
		org.springframework.cloud.netflix.zuul.SimpleZuulProxyApplicationTests.class,
		org.springframework.cloud.netflix.zuul.SimpleZuulServerApplicationTests.class,
		org.springframework.cloud.netflix.zuul.test.ZuulApacheHttpClientConfigurationTests.class,
		org.springframework.cloud.netflix.zuul.test.ZuulOkHttpClientConfigurationTests.class,
		org.springframework.cloud.netflix.zuul.web.ZuulHandlerMappingTests.class,
		org.springframework.cloud.netflix.zuul.ZuulFilterInitializerTests.class,
		org.springframework.cloud.netflix.zuul.ZuulProxyApplicationTests.class,
		org.springframework.cloud.netflix.zuul.ZuulProxyAutoConfigurationTests.class,
		org.springframework.cloud.netflix.zuul.ZuulProxyConfigurationTests.class,
		org.springframework.cloud.netflix.zuul.ZuulServerAutoConfigurationTests.class,
})
@Ignore
public class AdhocZuulTestSuite {

}
