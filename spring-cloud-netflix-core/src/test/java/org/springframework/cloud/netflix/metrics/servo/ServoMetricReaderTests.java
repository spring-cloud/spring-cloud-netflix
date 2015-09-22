/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.cloud.netflix.metrics.servo;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.MonitorConfig;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cloud.netflix.metrics.AbstractMetricsTests;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Jon Schneider
 */
public class ServoMetricReaderTests extends AbstractMetricsTests {
    @Test
    public void multipleMetricsReportedForSingleTimer() {
        MonitorRegistry registry = DefaultMonitorRegistry.getInstance();
        BasicTimer t = new BasicTimer(MonitorConfig.builder("t").build());
        registry.register(t);
        t.record(1000, TimeUnit.MILLISECONDS);

        ServoMetricReader reader = new ServoMetricReader(registry);
        reader.monitorRegistry = registry;

        int count = 0;
        for (Metric<?> metric : reader.findAll()) {
            count++;
        }

        assertEquals(4, count);
        assertEquals(4, reader.count());
    }
}
