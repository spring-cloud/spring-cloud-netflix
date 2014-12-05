package org.springframework.cloud.netflix.feign;

import static org.junit.Assert.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringDecoderTests.Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "spring.application.name=springdecodertest", "spring.jmx.enabled=true" })
@DirtiesContext
public class SpringDecoderTests extends FeignConfiguration {

    @Value("${local.server.port}")
    private int port = 0;

    public TestClient testClient() {
        return feign().target(TestClient.class, "http://localhost:"+port);
    }

    protected static interface TestClient {
        @RequestMapping(method = RequestMethod.GET, value = "/hello")
        public Hello getHello();

        @RequestMapping(method = RequestMethod.GET, value = "/hellos")
        public List<Hello> getHellos();

        @RequestMapping(method = RequestMethod.GET, value = "/hellostrings")
        public List<String> getHelloStrings();
    }

    @Configuration
    @EnableAutoConfiguration
    @RestController
    protected static class Application implements TestClient {

        public Hello getHello() {
            return new Hello("hello world 1");
        }

        public List<Hello> getHellos() {
            ArrayList<Hello> hellos = new ArrayList<>();
            hellos.add(new Hello("hello world 1"));
            hellos.add(new Hello("oi terra 2"));
            return hellos;
        }

        public List<String> getHelloStrings() {
            ArrayList<String> hellos = new ArrayList<>();
            hellos.add("hello world 1");
            hellos.add("oi terra 2");
            return hellos;
        }

        public static void main(String[] args) {
            new SpringApplicationBuilder(Application.class).properties(
                    "spring.application.name=springdecodertest", "management.contextPath=/admin")
                    .run(args);
        }
    }

    @Test
    public void testSimpleType() {
        Hello hello = testClient().getHello();
        assertNotNull("hello was null", hello);
        assertEquals("first hello didn't match", new Hello("hello world 1"), hello);
    }

    @Test
    public void testUserParameterizedTypeDecode() {
        List<Hello> hellos = testClient().getHellos();
        assertNotNull("hellos was null", hellos);
        assertEquals("hellos was not the right size", 2, hellos.size());
        assertEquals("first hello didn't match", new Hello("hello world 1"), hellos.get(0));
    }

    @Test
    public void testSimpleParameterizedTypeDecode() {
        List<String> hellos = testClient().getHelloStrings();
        assertNotNull("hellos was null", hellos);
        assertEquals("hellos was not the right size", 2, hellos.size());
        assertEquals("first hello didn't match", "hello world 1", hellos.get(0));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Hello {
        private String message;
    }
}
