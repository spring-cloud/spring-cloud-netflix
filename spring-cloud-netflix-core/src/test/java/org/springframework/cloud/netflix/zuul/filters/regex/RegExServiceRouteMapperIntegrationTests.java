package org.springframework.cloud.netflix.zuul.filters.regex;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.regex.RegExServiceRouteMapperIntegrationTests.SERVICE_ID;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.RoutesEndpoint;
import org.springframework.cloud.netflix.zuul.ZuulProxyConfiguration;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleCustomZuulProxyApplication.class)
@WebIntegrationTest(value = { "spring.application.name=regex-test-application",
        "spring.jmx.enabled=true"}, randomPort = true)
@TestPropertySource(properties = {
        "zuul.regexMapper=true",
        "zuul.regexMap.servicePattern=(?<domain>^.*)-(?<name>.*)-(?<version>v.*$)",
        "zuul.regexMap.routePattern=${version}/${domain}/${name}"})
@DirtiesContext
public class RegExServiceRouteMapperIntegrationTests {

    protected static final String SERVICE_ID="domain-service-v1";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ProxyRouteLocator routes;

    @Autowired
    private RoutesEndpoint endpoint;

    @Test
    public void getRegexMappedService() {
        endpoint.reset();
        ResponseEntity<String> result = new TestRestTemplate().exchange(
                    "http://localhost:" + this.port + "/v1/domain/service/get/1", HttpMethod.GET,
                new HttpEntity<>((Void) null), String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Get 1", result.getBody());
    }

    @Test
    public void getStaticRoute() {
        this.routes.addRoute("/self/**", "http://localhost:" + this.port);
        endpoint.reset();
        ResponseEntity<String> result = new TestRestTemplate().exchange(
                "http://localhost:" + this.port + "/self/get/1", HttpMethod.GET,
                new HttpEntity<>((Void) null), String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Get 1", result.getBody());
    }

}

@Configuration
@EnableAutoConfiguration
@RestController
@RibbonClient(value = SERVICE_ID, configuration = SimpleRibbonClientConfiguration.class)
class SampleCustomZuulProxyApplication {

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    public String get(@PathVariable String id) {
        return "Get " + id;
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleCustomZuulProxyApplication.class, args);
    }

    @Configuration
    @EnableZuulProxy
    protected static class CustomZuulProxyConfig extends ZuulProxyConfiguration {
        @Autowired
        private ZuulProperties zuulProperties;
        @Autowired
        private ServerProperties server;

        @Bean
        @Override
        public ProxyRouteLocator routeLocator() {
            DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
            List<String> services = new ArrayList<>();
            services.add(SERVICE_ID);
            when(discoveryClient.getServices()).thenReturn(services);
            return new ProxyRouteLocator(this.server.getServletPrefix(), discoveryClient,
                    this.zuulProperties);
        }
    }


}

@Configuration
class SimpleRibbonClientConfiguration {

    @Value("${local.server.port}")
    private int port = 0;

    @Bean
    public ServerList<Server> ribbonServerList() {
        return new StaticServerList<>(new Server("localhost", this.port));
    }
}
