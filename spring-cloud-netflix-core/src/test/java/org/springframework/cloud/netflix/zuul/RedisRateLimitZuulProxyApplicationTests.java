/*
 *  Copyright 2013-2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.cloud.netflix.zuul;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import redis.embedded.RedisServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.RateLimitConfiguration;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.RateLimitFilter;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.RateLimiter;
import org.springframework.cloud.netflix.zuul.filters.pre.ratelimit.redis.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vinicius Carvalho
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = {RedisTemplateConfiguration.class, RedisRateLimitZuulApplication.class})
@IntegrationTest({"server.port: 0",

		"zuul.ratelimit.enabled: true"
})
@DirtiesContext
public class RedisRateLimitZuulProxyApplicationTests {

	static RedisServer redisServer;

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private RateLimiter rateLimiter;

	@Autowired
	private ProxyRouteLocator routes;

	@Autowired
	private RoutesEndpoint endpoint;

	@BeforeClass
	public static void setup() throws Exception {
		redisServer = new RedisServer(6767);
		redisServer.start();
	}

	@AfterClass
	public static void shutdown() throws Exception {
		redisServer.stop();
	}

	@Test
	public void getUnauthenticated() {
		routes.addRoute("/self/**", "http://localhost:" + this.port + "/local");
		this.endpoint.reset();
		ResponseEntity<String> response = new TestRestTemplate().getForEntity("http://localhost:" + port + "/self/1", String.class);
		assertNotNull(response.getHeaders().get(RateLimitFilter.Headers.LIMIT));
		assertNotNull(response.getHeaders().get(RateLimitFilter.Headers.REMAINING));
		assertNotNull(response.getHeaders().get(RateLimitFilter.Headers.RESET));
	}

	@Test
	public void contextLoads() throws Exception {
		Assert.assertTrue(rateLimiter.getClass().isAssignableFrom(RedisRateLimiter.class));
	}

}

@Configuration
class RedisTemplateConfiguration {
	@Bean
	public RedisTemplate redisTemplate(JedisConnectionFactory cf) {
		RedisTemplate redisTemplate = new RedisTemplate();
		redisTemplate.setConnectionFactory(cf);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
		JedisConnectionFactory factory = new JedisConnectionFactory();
		factory.setHostName("localhost");
		factory.setPort(6767);
		factory.setUsePool(true);
		return factory;
	}

}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@ComponentScan(basePackageClasses = RateLimitConfiguration.class)
class RedisRateLimitZuulApplication {


	public static void main(String[] args) {
		SpringApplication.run(RateLimitZuulApplication.class);
	}

	@RequestMapping(value = "/local/{id}", method = RequestMethod.GET)
	public String get(@PathVariable String id) {
		return "Gotten " + id + "!";
	}
}