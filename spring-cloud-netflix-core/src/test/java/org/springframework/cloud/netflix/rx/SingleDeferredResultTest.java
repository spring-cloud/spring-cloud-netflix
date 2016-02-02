/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.rx;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.rx.beans.EventDto;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rx.Single;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

/**
 * Tests the {@link SingleDeferredResult} class.
 *
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SingleDeferredResultTest.Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0"})
@DirtiesContext
public class SingleDeferredResultTest {

    @Value("${local.server.port}")
    private int port = 0;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Configuration
    @EnableAutoConfiguration
    @RestController
    protected static class Application {

        @RequestMapping(method = RequestMethod.GET, value = "/single")
        public SingleDeferredResult<String> single() {
            return new SingleDeferredResult<>(Single.just("single value"));
        }

        @RequestMapping(method = RequestMethod.GET, value = "/singleWithResponse")
        public SingleDeferredResult<ResponseEntity<String>> singleWithResponse() {
            return new SingleDeferredResult<>(
                    Single.just(new ResponseEntity<>("single value", HttpStatus.NOT_FOUND))
            );
        }

        @RequestMapping(method = RequestMethod.GET, value = "/event", produces = APPLICATION_JSON_UTF8_VALUE)
        public SingleDeferredResult<EventDto> event() {
            return new SingleDeferredResult<>(Single.just(new EventDto("Spring.io", new Date())));
        }

        @RequestMapping(method = RequestMethod.GET, value = "/throw")
        public SingleDeferredResult<Object> error() {
            return new SingleDeferredResult<>(Single.error(new RuntimeException("Unexpected")));
        }
    }

    @Test
    public void shouldRetrieveSingleValue() {

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(path("/single"), String.class);

        // then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("single value", response.getBody());
    }

    @Test
    public void shouldRetrieveSingleValueWithStatusCode() {

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(path("/singleWithResponse"), String.class);

        // then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("single value", response.getBody());
    }

    @Test
    public void shouldRetrieveJsonSerializedPojoValue() {

        // when
        ResponseEntity<EventDto> response = restTemplate.getForEntity(path("/event"), EventDto.class);

        // then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Spring.io", response.getBody().getName());
    }

    @Test
    public void shouldRetrieveErrorResponse() {

        // when
        ResponseEntity<Object> response = restTemplate.getForEntity(path("/throw"), Object.class);

        // then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    private String path(String context) {
        return String.format("http://localhost:%d%s", port, context);
    }
}