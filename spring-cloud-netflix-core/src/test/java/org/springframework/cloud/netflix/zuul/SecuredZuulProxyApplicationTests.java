package org.springframework.cloud.netflix.zuul;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SecuredZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port: 0",
        "zuul.routes.other: /test/**=http://localhost:7777/local",
        "zuul.routes.another: /another/twolevel/**", "zuul.routes.simple: /simple/**",
        "zuul.routes.badhost: /badhost/**", "zuul.ignoredHeaders: X-Header",
        "zuul.routes.rnd: /rnd/**", "rnd.ribbon.listOfServers: ${random.value}",
        "zuul.removeSemicolonContent: false" })
@DirtiesContext
public class SecuredZuulProxyApplicationTests extends ZuulProxyTestBase {
    @Test
    public void testSecuredPost() throws Exception {
        ZuulProperties.ZuulRoute route = new ZuulProperties.ZuulRoute("self", "/self/**", null, "http://localhost:" + this.port + "/", true, false, Collections.<String> emptySet());
        route.setSensitiveHeaders(Collections.<String> emptySet());
        this.routes.addRoute(route);
        this.endpoint.reset();
        MultiValueMap<String, String> oauthFormData = new LinkedMultiValueMap<>();
        oauthFormData.add("grant_type", "password");
        oauthFormData.add("username", "greg");
        oauthFormData.add("password", "turnquist");
        oauthFormData.add("scope", "read");
        HttpHeaders oauthHeaders = new HttpHeaders();
        oauthHeaders.add("Authorization", "Basic " + new String(Base64.encode("foo:bar".getBytes())));
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<OAuth2AccessToken> tokenResponseEntity = template.postForEntity(
                "http://localhost:" + this.port + "/oauth/token",
                new HttpEntity<>(oauthFormData, oauthHeaders), OAuth2AccessToken.class);
        assertEquals(HttpStatus.OK, tokenResponseEntity.getStatusCode());
        HttpHeaders protectedHeaders = new HttpHeaders();
        protectedHeaders.add("Authorization", "Bearer " + tokenResponseEntity.getBody().getValue());

        ResponseEntity<String> result = template.exchange(
                "http://localhost:" + this.port + "/self/protected", HttpMethod.POST,
                new HttpEntity<>((Void) null, protectedHeaders), String.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("/protected", result.getBody());
    }

    @Test
    public void testSecuredPostInvalidToken() throws Exception {
        ZuulProperties.ZuulRoute route = new ZuulProperties.ZuulRoute("self", "/self/**", null, "http://localhost:" + this.port + "/", true, false, Collections.<String> emptySet());
        route.setSensitiveHeaders(Collections.<String> emptySet());
        this.routes.addRoute(route);
        this.endpoint.reset();
        TestRestTemplate template = new TestRestTemplate();
        HttpHeaders protectedHeaders = new HttpHeaders();
        protectedHeaders.add("Authorization", "Bearer junk");

        ResponseEntity<String> result = template.exchange(
                "http://localhost:" + this.port + "/self/protected", HttpMethod.POST,
                new HttpEntity<>((Void) null, protectedHeaders), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }
}

// Don't use @SpringBootApplication because we don't want to component scan
@Configuration
@EnableAutoConfiguration
@RestController
@EnableZuulProxy
@EnableAuthorizationServer
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RibbonClients({
        @RibbonClient(name = "badhost", configuration = SampleZuulProxyApplication.BadHostRibbonClientConfiguration.class),
        @RibbonClient(name = "simple", configuration = SimpleRibbonClientConfiguration.class),
        @RibbonClient(name = "another", configuration = AnotherRibbonClientConfiguration.class) })
class SecuredZuulProxyApplication extends ZuulProxyTestBase.AbstractZuulProxyApplication implements ResourceServerConfigurer {
    public static void main(String[] args) {
        SpringApplication.run(SecuredZuulProxyApplication.class, args);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/protected")
    public String protectedMethod(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resourceServerSecurityConfigurer) throws Exception {
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/protected").authorizeRequests().anyRequest().authenticated();
    }
}
