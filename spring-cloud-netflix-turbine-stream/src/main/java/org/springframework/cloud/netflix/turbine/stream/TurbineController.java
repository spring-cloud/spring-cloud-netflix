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

package org.springframework.cloud.netflix.turbine.stream;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import com.netflix.turbine.aggregator.InstanceKey;
import com.netflix.turbine.aggregator.StreamAggregator;
import com.netflix.turbine.internal.JsonUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.subjects.PublishSubject;

@RestController
public class TurbineController {
    private static final Log log = LogFactory.getLog(TurbineController.class);

    private final Flux<String> flux;

    public TurbineController(PublishSubject<Map<String, Object>> hystrixSubject) {
        Observable<Map<String, Object>> stream = StreamAggregator.aggregateGroupedStreams(hystrixSubject.groupBy(
                data -> InstanceKey.create((String) data.get("instanceId"))))
                .doOnUnsubscribe(() -> log.info("Unsubscribing aggregation."))
                .doOnSubscribe(() -> log.info("Starting aggregation")).flatMap(o -> o);
        Flux<Map<String, Object>> ping = Flux.interval(Duration.ofSeconds(5), Duration.ofSeconds(10))
                .map(l -> Collections.singletonMap("type", (Object) "ping"))
                .share();
        flux = Flux.merge(RxReactiveStreams.toPublisher(stream), ping)
                .share().map(map -> JsonUtility.mapToJson(map));
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream() {
        return this.flux;
    }
}
