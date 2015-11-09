package org.springframework.cloud.netflix.metrics.servo;

import com.google.common.collect.Lists;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.MonitorConfig;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cloud.netflix.metrics.AbstractMetricsTests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class ServoMetricReaderTests extends AbstractMetricsTests {
    @Test
    public void singleCompositeMonitorYieldsMultipleActuatorMetrics() {
        MonitorRegistry registry = DefaultMonitorRegistry.getInstance();

        ServoMetricReader reader = new ServoMetricReader(registry, new DimensionalServoMetricNaming());

        MonitorConfig.Builder builder = new MonitorConfig.Builder("metricName");

        BasicTimer timer = ServoMonitorCache.getTimer(builder.build());

        List<Metric<?>> metrics = Lists.newArrayList(reader.findAll());

        List<String> metricNames = new ArrayList<>();
        for (Metric<?> metric : metrics) {
            metricNames.add(metric.getName());
        }
        Collections.sort(metricNames);

        assertEquals(4, metrics.size());
        assertEquals("metricName(statistic=count,type=NORMALIZED,unit=MILLISECONDS)", metricNames.get(0));
        assertEquals("metricName(statistic=max,type=GAUGE,unit=MILLISECONDS)", metricNames.get(1));
        assertEquals("metricName(statistic=min,type=GAUGE,unit=MILLISECONDS)", metricNames.get(2));
        assertEquals("metricName(statistic=totalTime,type=NORMALIZED,unit=MILLISECONDS)", metricNames.get(3));
    }
}
