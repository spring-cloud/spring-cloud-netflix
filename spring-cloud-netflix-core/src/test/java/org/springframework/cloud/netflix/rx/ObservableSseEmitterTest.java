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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

/**
 * Tests the {@link ObservableSseEmitter} class.
 *
 * @author Spencer Gibb
 * @author Jakub Narloch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ObservableSseEmitterTest.Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0"})
@DirtiesContext
public class ObservableSseEmitterTest {

    @Value("${local.server.port}")
    private int port = 0;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Configuration
    @EnableAutoConfiguration
    @RestController
    protected static class Application {

        @RequestMapping(method = RequestMethod.GET, value = "/sse")
        public SseEmitter single() {
            return RxResponse.sse(Observable.just("single value"));
        }

        @RequestMapping(method = RequestMethod.GET, value = "/messages")
        public SseEmitter messages() {
            return RxResponse.sse(Observable.just("message 1", "message 2", "message 3"));
        }

        @RequestMapping(method = RequestMethod.GET, value = "/events")
        public SseEmitter event() {
            return RxResponse.sse(APPLICATION_JSON_UTF8, Observable.just(
                    new EventDto("Spring.io", getDate(2016, 4, 11)),
                    new EventDto("JavaOne", getDate(2016, 8, 22))
            ));
        }
    }

    @Test
    public void shouldRetrieveSse() {

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(path("/sse"), String.class);

        // then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("data:single value\n\n", response.getBody());
    }

    @Test
    public void shouldRetrieveSseWithMultipleMessages() {

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(path("/messages"), String.class);

        // then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("data:message 1\n\ndata:message 2\n\ndata:message 3\n\n", response.getBody());
    }

    @Test
    public void shouldRetrieveJsonOverSseWithMultipleMessages() {

        // when
        ResponseEntity<String> response = restTemplate.getForEntity(path("/events"), String.class);

        // then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("data:{\"name\":\"Spring.io\",\"date\":1462968000000}\n\ndata:{\"name\":\"JavaOne\",\"date\":1474545600000}\n\n", response.getBody());
    }

    private String path(String context) {
        return String.format("http://localhost:%d%s", port, context);
    }

    private static Date getDate(int year, int month, int day) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day, 12, 0, 0);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return calendar.getTime();
    }
}