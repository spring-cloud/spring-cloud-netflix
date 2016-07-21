package org.springframework.cloud.netflix.resttemplate;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.niws.client.http.HttpClientLoadBalancerErrorHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RestTemplateRetryTest.Application.class)
@WebIntegrationTest(randomPort = true, value = {
		"spring.application.name=resttemplatetest",
		"logging.level.org.springframework.cloud.netflix.resttemplate=DEBUG",
		"badClients.ribbon.MaxAutoRetries=0",
		"badClients.ribbon.OkToRetryOnAllOperations=true", "ribbon.http.client.enabled" })
@DirtiesContext
public class RestTemplateRetryTest {

	final private static Log logger = LogFactory.getLog(RestTemplateRetryTest.class);

	@Value("${local.server.port}")
	private int port = 0;

	@Autowired
	private RestTemplate testClient;

	public RestTemplateRetryTest() {
	}

	@Configuration
	@EnableAutoConfiguration
	@RestController
	@RibbonClient(name = "badClients", configuration = LocalBadClientConfiguration.class)
	public static class Application {

		private AtomicInteger hits = new AtomicInteger(1);
		private AtomicInteger retryHits = new AtomicInteger(1);

		@RequestMapping(method = RequestMethod.GET, value = "/ping")
		public int ping() {
			return 0;
		}

		@RequestMapping(method = RequestMethod.GET, value = "/good")
		public int good() {
			int lValue = this.hits.getAndIncrement();
			return lValue;
		}

		@RequestMapping(method = RequestMethod.GET, value = "/timeout")
		public int timeout() throws Exception {
			int lValue = this.retryHits.getAndIncrement();

			// Force the good server to have 2 consecutive errors a couple of times.
			if (lValue == 2 || lValue == 3 || lValue == 5 || lValue == 6) {
				Thread.sleep(500);
			}
			return lValue;
		}

		@RequestMapping(method = RequestMethod.GET, value = "/null")
		public int isNull() throws Exception {
			throw new NullPointerException("Null");
		}

		@LoadBalanced
		@Bean
		RestTemplate restTemplate() {
			return new RestTemplate();
		}
	}

	@Before
	public void setup() throws Exception {
		// Force Ribbon configuration by making one call.
		this.testClient.getForObject("http://badClients/ping", Integer.class);
	}

	@Test
	public void testNullPointer() throws Exception {

		LoadBalancerStats stats = LocalBadClientConfiguration.balancer
				.getLoadBalancerStats();
		ServerStats badServer1Stats = stats
				.getSingleServerStat(LocalBadClientConfiguration.badServer);
		ServerStats badServer2Stats = stats
				.getSingleServerStat(LocalBadClientConfiguration.badServer2);
		ServerStats goodServerStats = stats
				.getSingleServerStat(LocalBadClientConfiguration.goodServer);

		badServer1Stats.clearSuccessiveConnectionFailureCount();
		badServer2Stats.clearSuccessiveConnectionFailureCount();
		long targetConnectionCount = goodServerStats.getTotalRequestsCount() + 10;

		// A null pointer should NOT trigger a circuit breaker.
		for (int index = 0; index < 10; index++) {
			try {
				this.testClient.getForObject("http://badClients/null", Integer.class);
			}
			catch (Exception exception) {
			}
		}
		logServerStats(LocalBadClientConfiguration.badServer);
		logServerStats(LocalBadClientConfiguration.badServer2);
		logServerStats(LocalBadClientConfiguration.goodServer);

		assertTrue(badServer1Stats.isCircuitBreakerTripped());
		assertTrue(badServer2Stats.isCircuitBreakerTripped());
		assertEquals(targetConnectionCount, goodServerStats.getTotalRequestsCount());

		// Wait for any timeout thread to finish.

	}

	private void logServerStats(Server server) {
		LoadBalancerStats stats = LocalBadClientConfiguration.balancer
				.getLoadBalancerStats();
		ServerStats serverStats = stats.getSingleServerStat(server);
		logger.debug("Server : " + server.toString() + " : Total Count == "
				+ serverStats.getTotalRequestsCount() + ", Failure Count == "
				+ serverStats.getFailureCount() + ", Successive Connection Failure == "
				+ serverStats.getSuccessiveConnectionFailureCount()
				+ ", Circuit Breaker ? == " + serverStats.isCircuitBreakerTripped());
	}

	@Test
	public void testRestRetries() {

		LoadBalancerStats stats = LocalBadClientConfiguration.balancer
				.getLoadBalancerStats();
		ServerStats badServer1Stats = stats
				.getSingleServerStat(LocalBadClientConfiguration.badServer);
		ServerStats badServer2Stats = stats
				.getSingleServerStat(LocalBadClientConfiguration.badServer2);
		ServerStats goodServerStats = stats
				.getSingleServerStat(LocalBadClientConfiguration.goodServer);

		badServer1Stats.clearSuccessiveConnectionFailureCount();
		badServer2Stats.clearSuccessiveConnectionFailureCount();
		long targetConnectionCount = goodServerStats.getTotalRequestsCount() + 20;

		int hits = 0;

		for (int index = 0; index < 20; index++) {
			hits = this.testClient.getForObject("http://badClients/good", Integer.class);
		}

		logServerStats(LocalBadClientConfiguration.badServer);
		logServerStats(LocalBadClientConfiguration.badServer2);
		logServerStats(LocalBadClientConfiguration.goodServer);

		assertTrue(badServer1Stats.isCircuitBreakerTripped());
		assertTrue(badServer2Stats.isCircuitBreakerTripped());
		assertEquals(targetConnectionCount, goodServerStats.getTotalRequestsCount());
		assertEquals(20, hits);
		System.out.println("Retry Hits: " + hits);
	}

	@Test
	public void testRestRetriesWithReadTimeout() throws Exception {

		LoadBalancerStats stats = LocalBadClientConfiguration.balancer
				.getLoadBalancerStats();
		ServerStats badServer1Stats = stats
				.getSingleServerStat(LocalBadClientConfiguration.badServer);
		ServerStats badServer2Stats = stats
				.getSingleServerStat(LocalBadClientConfiguration.badServer2);
		ServerStats goodServerStats = stats
				.getSingleServerStat(LocalBadClientConfiguration.goodServer);

		badServer1Stats.clearSuccessiveConnectionFailureCount();
		badServer2Stats.clearSuccessiveConnectionFailureCount();
		assertTrue(!badServer1Stats.isCircuitBreakerTripped());
		assertTrue(!badServer2Stats.isCircuitBreakerTripped());

		int hits = 0;

		for (int index = 0; index < 15; index++) {
			hits = this.testClient.getForObject("http://badClients/timeout",
					Integer.class);
		}
		logServerStats(LocalBadClientConfiguration.badServer);
		logServerStats(LocalBadClientConfiguration.badServer2);
		logServerStats(LocalBadClientConfiguration.goodServer);

		assertTrue(badServer1Stats.isCircuitBreakerTripped());
		assertTrue(badServer2Stats.isCircuitBreakerTripped());
		assertTrue(!goodServerStats.isCircuitBreakerTripped());

		// 15 + 4 timeouts. See the endpoint for timeout conditions.
		assertEquals(19, hits);

		// Wait for any timeout thread to finish.
		Thread.sleep(600);

	}

}

// Load balancer with fixed server list for "local" pointing to localhost
// and some bogus servers are thrown in to test retry
@Configuration
class LocalBadClientConfiguration {

	static BaseLoadBalancer balancer;
	static Server goodServer;
	static Server badServer;
	static Server badServer2;

	public LocalBadClientConfiguration() {
	}

	@Value("${local.server.port}")
	private int port = 0;

	@Bean
	public IRule loadBalancerRule() {
		// This is a good place to try different load balancing rules and how those rules
		// behave in failure
		// states: BestAvailableRule, WeightedResponseTimeRule, etc

		// This rule just uses a round robin and will skip servers that are in circuit
		// breaker state.
		return new AvailabilityFilteringRule();

	}

	@Bean
	public ILoadBalancer ribbonLoadBalancer(IClientConfig config,
			ServerList<Server> serverList, IRule rule, IPing ping) {

		goodServer = new Server("localhost", this.port);
		badServer = new Server("mybadhost", 10001);
		badServer2 = new Server("localhost", -1);

		balancer = LoadBalancerBuilder.newBuilder().withClientConfig(config)
				.withRule(rule).withPing(ping).buildFixedServerListLoadBalancer(
						Arrays.asList(badServer, badServer2, goodServer));
		return balancer;
	}

	@Bean
	public RetryHandler retryHandler() {
		return new OverrideRetryHandler();
	}

	static class OverrideRetryHandler extends HttpClientLoadBalancerErrorHandler {
		public OverrideRetryHandler() {
			this.circuitRelated.add(UnknownHostException.class);
			this.retriable.add(UnknownHostException.class);

		}
	}

}
