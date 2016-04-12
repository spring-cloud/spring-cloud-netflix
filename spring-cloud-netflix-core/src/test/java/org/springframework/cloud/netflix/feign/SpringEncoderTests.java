/*
 * Copyright 2013-2015 the original author or authors.
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
 */

package org.springframework.cloud.netflix.feign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringEncoderTests.Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "spring.application.name=springencodertest",
    "spring.jmx.enabled=true"})
@DirtiesContext
public class SpringEncoderTests extends FeignClientFactoryBean {

    @Autowired
    FeignContext context;

    @Value("${local.server.port}")
    private int port = 0;

    public SpringEncoderTests() {
        setName("test");
    }

    public TestClient testClient() {
        setType(this.getClass());
        setDecode404(false);
        return feign(context).target(TestClient.class, "http://localhost:" + this.port);
    }

    @Test
    public void testMultipart() {

        MultiValueMap<String, Object> personData = new LinkedMultiValueMap<>();
        personData.set("name", "John");
        personData.set("lastName", "Do");

        ResponseEntity<String> fullName = testClient().fullName(personData);

        assertEquals("John Do", fullName.getBody());
    }

    protected interface TestClient {

        @RequestMapping(method = RequestMethod.POST, value = "/fullName", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<String> fullName(MultiValueMap<String, Object> params);

    }

    @Configuration
    @EnableAutoConfiguration
    @RestController
    protected static class Application {

        @RequestMapping(method = RequestMethod.POST, value = "/fullName", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<String> fullName(String name, String lastName) {
            return ResponseEntity.ok(name + " " + lastName);
        }

        public static void main(String[] args) {
            new SpringApplicationBuilder(Application.class).properties(
                "spring.application.name=springencodertest",
                "management.contextPath=/admin").run(args);
        }

    }

}
