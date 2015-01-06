package org.springframework.netflix.hystrix.amqp;

import com.netflix.hystrix.*;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Spencer Gibb
 * see com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller.MetricsPoller
 */
@Slf4j
public class HystrixStreamTask {

    @Autowired
    private HystrixStreamChannel channel;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ApplicationContext context;

    private final LinkedBlockingQueue<String> jsonMetrics = new LinkedBlockingQueue<>(1000);

    private final JsonFactory jsonFactory = new JsonFactory();

    //TODO: use integration to split this up?
    @Scheduled(fixedRateString = "${hystrix.stream.amqp.sendRate:500}")
    public void sendMetrics() {
        ArrayList<String> metrics = new ArrayList<>();
        jsonMetrics.drainTo(metrics);

        if (!metrics.isEmpty()) {
            log.trace("sending amqp metrics size: " + metrics.size());
            for (String json: metrics) {
                //TODO: batch all metrics to one message
                try {
                    channel.send(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //@InboundChannelAdapter()
    //TODO: move fixedRate to configuration
    @Scheduled(fixedRateString = "${hystrix.stream.amqp.gatherRate:500}")
    public void gatherMetrics() {
        try {
            // command metrics
            Collection<HystrixCommandMetrics> instances = HystrixCommandMetrics.getInstances();
            if (!instances.isEmpty()) {
                log.trace("gathering metrics size: " + instances.size());
            }
            for (HystrixCommandMetrics commandMetrics : instances) {
                HystrixCommandKey key = commandMetrics.getCommandKey();
                HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(key);

                StringWriter jsonString = new StringWriter();
                JsonGenerator json = jsonFactory.createJsonGenerator(jsonString);

                json.writeStartObject();

                addServiceData(json);
                json.writeObjectFieldStart("data");
                json.writeStringField("type", "HystrixCommand");
                json.writeStringField("name", key.name());
                json.writeStringField("group", commandMetrics.getCommandGroup().name());
                json.writeNumberField("currentTime", System.currentTimeMillis());

                // circuit breaker
                if (circuitBreaker == null) {
                    // circuit breaker is disabled and thus never open
                    json.writeBooleanField("isCircuitBreakerOpen", false);
                } else {
                    json.writeBooleanField("isCircuitBreakerOpen", circuitBreaker.isOpen());
                }
                HystrixCommandMetrics.HealthCounts healthCounts = commandMetrics.getHealthCounts();
                json.writeNumberField("errorPercentage", healthCounts.getErrorPercentage());
                json.writeNumberField("errorCount", healthCounts.getErrorCount());
                json.writeNumberField("requestCount", healthCounts.getTotalRequests());

                // rolling counters
                json.writeNumberField("rollingCountCollapsedRequests", commandMetrics.getRollingCount(HystrixRollingNumberEvent.COLLAPSED));
                json.writeNumberField("rollingCountExceptionsThrown", commandMetrics.getRollingCount(HystrixRollingNumberEvent.EXCEPTION_THROWN));
                json.writeNumberField("rollingCountFailure", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FAILURE));
                json.writeNumberField("rollingCountFallbackFailure", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FALLBACK_FAILURE));
                json.writeNumberField("rollingCountFallbackRejection", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FALLBACK_REJECTION));
                json.writeNumberField("rollingCountFallbackSuccess", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FALLBACK_SUCCESS));
                json.writeNumberField("rollingCountResponsesFromCache", commandMetrics.getRollingCount(HystrixRollingNumberEvent.RESPONSE_FROM_CACHE));
                json.writeNumberField("rollingCountSemaphoreRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
                json.writeNumberField("rollingCountShortCircuited", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SHORT_CIRCUITED));
                json.writeNumberField("rollingCountSuccess", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SUCCESS));
                json.writeNumberField("rollingCountThreadPoolRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED));
                json.writeNumberField("rollingCountTimeout", commandMetrics.getRollingCount(HystrixRollingNumberEvent.TIMEOUT));

                json.writeNumberField("currentConcurrentExecutionCount", commandMetrics.getCurrentConcurrentExecutionCount());

                // latency percentiles
                json.writeNumberField("latencyExecute_mean", commandMetrics.getExecutionTimeMean());
                json.writeObjectFieldStart("latencyExecute");
                json.writeNumberField("0", commandMetrics.getExecutionTimePercentile(0));
                json.writeNumberField("25", commandMetrics.getExecutionTimePercentile(25));
                json.writeNumberField("50", commandMetrics.getExecutionTimePercentile(50));
                json.writeNumberField("75", commandMetrics.getExecutionTimePercentile(75));
                json.writeNumberField("90", commandMetrics.getExecutionTimePercentile(90));
                json.writeNumberField("95", commandMetrics.getExecutionTimePercentile(95));
                json.writeNumberField("99", commandMetrics.getExecutionTimePercentile(99));
                json.writeNumberField("99.5", commandMetrics.getExecutionTimePercentile(99.5));
                json.writeNumberField("100", commandMetrics.getExecutionTimePercentile(100));
                json.writeEndObject();
                //
                json.writeNumberField("latencyTotal_mean", commandMetrics.getTotalTimeMean());
                json.writeObjectFieldStart("latencyTotal");
                json.writeNumberField("0", commandMetrics.getTotalTimePercentile(0));
                json.writeNumberField("25", commandMetrics.getTotalTimePercentile(25));
                json.writeNumberField("50", commandMetrics.getTotalTimePercentile(50));
                json.writeNumberField("75", commandMetrics.getTotalTimePercentile(75));
                json.writeNumberField("90", commandMetrics.getTotalTimePercentile(90));
                json.writeNumberField("95", commandMetrics.getTotalTimePercentile(95));
                json.writeNumberField("99", commandMetrics.getTotalTimePercentile(99));
                json.writeNumberField("99.5", commandMetrics.getTotalTimePercentile(99.5));
                json.writeNumberField("100", commandMetrics.getTotalTimePercentile(100));
                json.writeEndObject();

                // property values for reporting what is actually seen by the command rather than what was set somewhere
                HystrixCommandProperties commandProperties = commandMetrics.getProperties();

                json.writeNumberField("propertyValue_circuitBreakerRequestVolumeThreshold", commandProperties.circuitBreakerRequestVolumeThreshold().get());
                json.writeNumberField("propertyValue_circuitBreakerSleepWindowInMilliseconds", commandProperties.circuitBreakerSleepWindowInMilliseconds().get());
                json.writeNumberField("propertyValue_circuitBreakerErrorThresholdPercentage", commandProperties.circuitBreakerErrorThresholdPercentage().get());
                json.writeBooleanField("propertyValue_circuitBreakerForceOpen", commandProperties.circuitBreakerForceOpen().get());
                json.writeBooleanField("propertyValue_circuitBreakerForceClosed", commandProperties.circuitBreakerForceClosed().get());
                json.writeBooleanField("propertyValue_circuitBreakerEnabled", commandProperties.circuitBreakerEnabled().get());

                json.writeStringField("propertyValue_executionIsolationStrategy", commandProperties.executionIsolationStrategy().get().name());
                json.writeNumberField("propertyValue_executionIsolationThreadTimeoutInMilliseconds", commandProperties.executionIsolationThreadTimeoutInMilliseconds().get());
                json.writeBooleanField("propertyValue_executionIsolationThreadInterruptOnTimeout", commandProperties.executionIsolationThreadInterruptOnTimeout().get());
                json.writeStringField("propertyValue_executionIsolationThreadPoolKeyOverride", commandProperties.executionIsolationThreadPoolKeyOverride().get());
                json.writeNumberField("propertyValue_executionIsolationSemaphoreMaxConcurrentRequests", commandProperties.executionIsolationSemaphoreMaxConcurrentRequests().get());
                json.writeNumberField("propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests", commandProperties.fallbackIsolationSemaphoreMaxConcurrentRequests().get());

                    /*
                     * The following are commented out as these rarely change and are verbose for streaming for something people don't change.
                     * We could perhaps allow a property or request argument to include these.
                     */

                //                    json.put("propertyValue_metricsRollingPercentileEnabled", commandProperties.metricsRollingPercentileEnabled().get());
                //                    json.put("propertyValue_metricsRollingPercentileBucketSize", commandProperties.metricsRollingPercentileBucketSize().get());
                //                    json.put("propertyValue_metricsRollingPercentileWindow", commandProperties.metricsRollingPercentileWindowInMilliseconds().get());
                //                    json.put("propertyValue_metricsRollingPercentileWindowBuckets", commandProperties.metricsRollingPercentileWindowBuckets().get());
                //                    json.put("propertyValue_metricsRollingStatisticalWindowBuckets", commandProperties.metricsRollingStatisticalWindowBuckets().get());
                json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", commandProperties.metricsRollingStatisticalWindowInMilliseconds().get());

                json.writeBooleanField("propertyValue_requestCacheEnabled", commandProperties.requestCacheEnabled().get());
                json.writeBooleanField("propertyValue_requestLogEnabled", commandProperties.requestLogEnabled().get());

                json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster

                json.writeEndObject(); // end data attribute
                json.writeEndObject();
                json.close();

                // output
                jsonMetrics.add(jsonString.getBuffer().toString());
            }

            // thread pool metrics
            for (HystrixThreadPoolMetrics threadPoolMetrics : HystrixThreadPoolMetrics.getInstances()) {
                HystrixThreadPoolKey key = threadPoolMetrics.getThreadPoolKey();

                StringWriter jsonString = new StringWriter();
                JsonGenerator json = jsonFactory.createJsonGenerator(jsonString);
                json.writeStartObject();

                addServiceData(json);
                json.writeObjectFieldStart("data");

                json.writeStringField("type", "HystrixThreadPool");
                json.writeStringField("name", key.name());
                json.writeNumberField("currentTime", System.currentTimeMillis());

                json.writeNumberField("currentActiveCount", threadPoolMetrics.getCurrentActiveCount().intValue());
                json.writeNumberField("currentCompletedTaskCount", threadPoolMetrics.getCurrentCompletedTaskCount().longValue());
                json.writeNumberField("currentCorePoolSize", threadPoolMetrics.getCurrentCorePoolSize().intValue());
                json.writeNumberField("currentLargestPoolSize", threadPoolMetrics.getCurrentLargestPoolSize().intValue());
                json.writeNumberField("currentMaximumPoolSize", threadPoolMetrics.getCurrentMaximumPoolSize().intValue());
                json.writeNumberField("currentPoolSize", threadPoolMetrics.getCurrentPoolSize().intValue());
                json.writeNumberField("currentQueueSize", threadPoolMetrics.getCurrentQueueSize().intValue());
                json.writeNumberField("currentTaskCount", threadPoolMetrics.getCurrentTaskCount().longValue());
                json.writeNumberField("rollingCountThreadsExecuted", threadPoolMetrics.getRollingCountThreadsExecuted());
                json.writeNumberField("rollingMaxActiveThreads", threadPoolMetrics.getRollingMaxActiveThreads());

                json.writeNumberField("propertyValue_queueSizeRejectionThreshold", threadPoolMetrics.getProperties().queueSizeRejectionThreshold().get());
                json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", threadPoolMetrics.getProperties().metricsRollingStatisticalWindowInMilliseconds().get());

                json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster

                json.writeEndObject(); // end of data object
                json.writeEndObject();
                json.close();
                // output to stream
                jsonMetrics.add(jsonString.getBuffer().toString());
            }
        } catch (Exception e) {
            log.error("Error adding metrics to queue", e);
        }
    }

    private void addServiceData(JsonGenerator json) throws IOException {
        ServiceInstance localService = discoveryClient.getLocalServiceInstance();
        json.writeObjectFieldStart("origin");
        json.writeStringField("host", localService.getHost());
        json.writeNumberField("port", localService.getPort());
        json.writeStringField("serviceId", localService.getServiceId());
        json.writeEndObject();
    }
}
