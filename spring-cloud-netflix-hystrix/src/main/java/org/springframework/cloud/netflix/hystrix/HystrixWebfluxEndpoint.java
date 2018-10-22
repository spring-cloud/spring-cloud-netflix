/*
 * Copyright 2013-2018 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.hystrix;

import java.time.Duration;

import org.reactivestreams.Publisher;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

import reactor.core.publisher.Flux;

/**
 * @author Spencer Gibb
 */
@RestControllerEndpoint(id = "hystrix.stream")
public class HystrixWebfluxEndpoint {

    private final Flux<String> stream;

    public HystrixWebfluxEndpoint(Publisher<String> dashboardData) {
        stream = Flux.interval(Duration.ofMillis(500)).map(aLong -> "{\"type\":\"ping\"}")
                .mergeWith(dashboardData).share();
    }

    // path needs to be empty, so it registers correct as /actuator/hystrix.stream
    @GetMapping(path = "", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> hystrixStream() {
        return stream;
    }
}
