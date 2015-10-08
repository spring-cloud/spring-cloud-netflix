package org.springframework.cloud.netflix.metrics.servo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;

public class ServoMetricReader implements MetricReader {
    MonitorRegistry monitorRegistry;

    public ServoMetricReader(MonitorRegistry monitorRegistry) {
        this.monitorRegistry = monitorRegistry;
    }

    @Override
    public Metric<?> findOne(String s) {
        throw new UnsupportedOperationException("cannot construct a tag-based Servo id from a hierarchical name");
    }

    @Override
    public Iterable<Metric<?>> findAll() {
        Collection<Metric<?>> metrics = new ArrayList<>();
        for (Monitor<?> monitor : monitorRegistry.getRegisteredMonitors()) {
            addToMetrics(monitor, metrics);
        }
        return metrics;
    }

    private static void addToMetrics(Monitor<?> monitor, Collection<Metric<?>> metrics) {
        if(monitor instanceof CompositeMonitor) {
            for (Monitor<?> nestedMonitor : ((CompositeMonitor<?>) monitor).getMonitors()) {
                addToMetrics(nestedMonitor, metrics);
            }
        }
        else if(monitor.getValue() instanceof Number) {
            // Servo does support non-numeric values, but there is no such concept in Spring Boot
            metrics.add(new Metric<>(asHierarchicalName(monitor), (Number) monitor.getValue()));
        }
    }

    private static String asHierarchicalName(Monitor<?> monitor) {
        MonitorConfig config = monitor.getConfig();
        List<String> tags = new ArrayList<>(config.getTags().size());
        for (Tag t : config.getTags()) {
            tags.add(t.getKey() + "=" + t.getValue());
        }
        return config.getName() + "(" + StringUtils.join(tags, ",") + ")";
    }

    @Override
    public long count() {
        long count = 0;
        for(Monitor<?> monitor: monitorRegistry.getRegisteredMonitors()) {
            count += countMetrics(monitor);
        }
        return count;
    }

    private static long countMetrics(Monitor<?> monitor) {
        if(monitor instanceof CompositeMonitor) {
            long count = 0;
            for (Monitor<?> nestedMonitor : ((CompositeMonitor<?>) monitor).getMonitors()) {
                count += countMetrics(nestedMonitor);
            }
            return count;
        }
        return 1;
    }
}
