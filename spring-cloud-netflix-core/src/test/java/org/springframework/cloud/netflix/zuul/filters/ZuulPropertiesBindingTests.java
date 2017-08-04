package org.springframework.cloud.netflix.zuul.filters;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ZuulPropertiesBindingTests {

    @Test
    public void testThatNewRoutesGetMapped() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment(context, "zuul.routes.client1=/path1/**");
        context.register(TestConfiguration.class);
        context.refresh();

        ZuulProperties zuulProperties = context.getBean(ZuulProperties.class);
        assertThat(zuulProperties.getRoutes()).containsKey("client1");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "zuul.routes.client2=/path2/**");

        context.publishEvent(new EnvironmentChangeEvent(new HashSet<>(Arrays.asList("zuul.routes.client2"))));
        assertThat(zuulProperties.getRoutes()).containsKey("client2");
    }

    @Test
    @Ignore
    // Ideally this test should pass - when a property is removed, it should be reflected in ZuulProperties or
    // broadly in any @ConfigurationProperties annotated bean
    public void testThatRemovedRoutesAreHandled() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment(context,
                "zuul.routes.client1=/path1/**", "zuul.routes.client2=/path2/**");
        context.register(TestConfiguration.class);
        context.refresh();

        ZuulProperties zuulProperties = context.getBean(ZuulProperties.class);

        assertThat(zuulProperties.getRoutes()).containsKeys("client1", "client2");
        removeEnv(context.getEnvironment(), "zuul.routes.client2");

        context.publishEvent(new EnvironmentChangeEvent(new HashSet<>(Arrays.asList("zuul.routes.client2"))));
        assertThat(zuulProperties.getRoutes()).doesNotContainKey("client2");
    }

    private void removeEnv(ConfigurableEnvironment environment, String... keys) {
        MutablePropertySources sources = environment.getPropertySources();
        Map<String, Object> map = (Map<String, Object>) sources.get("test").getSource();
        for (String key : keys) {
            map.remove(key);
        }
    }

    @Configuration
    @Import({ConfigurationPropertiesAutoConfiguration.class, ConfigurationPropertiesRebinderAutoConfiguration.class})
    static class TestConfiguration {

        @Bean
        public ZuulProperties zuulProperties() {
            return new ZuulProperties();
        }

    }
}
