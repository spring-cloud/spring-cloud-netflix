package org.springframework.cloud.netflix.zuul.filters.route;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(value = { "zuul.routes.myroute.service-id=eager",
		"zuul.ribbon.eager-load=true" })
@DirtiesContext
public class EagerLoadOfZuulConfigurationTests {

	@Test
	public void testEagerLoading() {
		// Child context FooConfig should have been eagerly instantiated..
		assertThat(Foo.getInstanceCount()).isEqualTo(1);
	}

	@EnableAutoConfiguration
	@Configuration
	@EnableZuulProxy
	@RibbonClients(@RibbonClient(name = "eager", configuration = FooConfig.class))
	static class TestConfig {

	}

	static class Foo {
		private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

		public Foo() {
			INSTANCE_COUNT.incrementAndGet();
		}

		public static int getInstanceCount() {
			return INSTANCE_COUNT.get();
		}
	}

	static class FooConfig {

		@Bean
		public Foo foo() {
			return new Foo();
		}

	}
}
