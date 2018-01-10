/*
 * Copyright 2014-2018 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix;

import org.reactivestreams.Publisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * @author Spencer Gibb
 */
@RestController
@RequestMapping("${management.endpoints.web.base-path:/actuator}${management.hystrix.path:/hystrix.stream}")
public class HystrixWebfluxController {

    private final Flux<String> stream;

    public HystrixWebfluxController(Publisher<String> dashboardData) {
        stream = Flux.interval(Duration.ofMillis(500)).map(aLong -> "{\"type\":\"ping\"}")
                .mergeWith(dashboardData).share();
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> hystrixStream() {
        return stream;
    }
}
