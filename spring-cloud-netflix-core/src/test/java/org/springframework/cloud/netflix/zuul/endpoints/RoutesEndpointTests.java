package org.springframework.cloud.netflix.zuul.endpoints;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.PatternServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.endpoints.RoutesEndpointTests.SERVICE_ID;

/**
 * Tests for Routes endpoint.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SampleCustomZuulProxyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {
        "spring.application.name=endpoints-test-application" })
@DirtiesContext
public class RoutesEndpointTests {

    protected static final String SERVICE_ID = "endpoint-service-v1";

    @Autowired
    private DiscoveryClientRouteLocator routes;

    @Autowired
    private RoutesEndpoint endpoint;

    @Value("${local.server.port}")
    private int port;

    @Before
    public void resetRoutes() {
        this.endpoint.reset();
    }

    @Test
    public void getStaticRoute() {
        final String location = "http://localhost:" + this.port;

        this.routes.addRoute("/self/**", location);
        assertEquals(location, this.endpoint.getRoutes().get("/self/**"));
    }

    @Test
    public void getDiscoveredRoute() {
        assertEquals(SERVICE_ID, this.endpoint.getRoutes().get("/v1/endpoint/service/**"));
    }
}

@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@RibbonClient(value = SERVICE_ID, configuration = SimpleRibbonClientConfiguration.class)
class SampleCustomZuulProxyApplication {

    @Bean
    public DiscoveryClient discoveryClient() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        List<String> services = new ArrayList<>();
        services.add(SERVICE_ID);
        when(discoveryClient.getServices()).thenReturn(services);
        return discoveryClient;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    public String get(@PathVariable String id) {
        return "Get " + id;
    }

    @Bean
    public PatternServiceRouteMapper serviceRouteMapper() {
        return new PatternServiceRouteMapper(
                "(?<domain>^.+)-(?<name>.+)-(?<version>v.+$)",
                "${version}/${domain}/${name}");
    }

    public static void main(String[] args) {
        SpringApplication.run(org.springframework.cloud.netflix.zuul.endpoints.SampleCustomZuulProxyApplication.class, args);
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