package org.springframework.cloud.netflix.ribbon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

/**
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RibbonAutoConfiguration.class,
		ArchaiusAutoConfiguration.class, RibbonApplicationContextInitializerTests.RibbonInitializerConfig.class})
@DirtiesContext
public class RibbonApplicationContextInitializerTests {

	@Autowired
	private SpringClientFactory springClientFactory;

	@Test
	public void testContextShouldInitalizeChildContexts() {

		// Context should have been initialized and an instance of Foo created
		assertThat(Foo.getInstanceCount()).isEqualTo(1);
		ApplicationContext ctx = springClientFactory.getContext("testspec");

		assertThat(Foo.getInstanceCount()).isEqualTo(1);
		Foo foo = ctx.getBean("foo", Foo.class);
		assertThat(foo).isNotNull();
	}

	@Configuration
	static class FooConfig {

		@Bean
		public Foo foo() {
			return new Foo();
		}

	}

	@Configuration
	@RibbonClient(name="testspec", configuration = FooConfig.class)
	static class RibbonInitializerConfig {

		@Bean
		public RibbonApplicationContextInitializer ribbonApplicationContextInitializer(
				SpringClientFactory springClientFactory) {
			return new RibbonApplicationContextInitializer(springClientFactory,
					Arrays.asList("testspec"));
		}

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
}
