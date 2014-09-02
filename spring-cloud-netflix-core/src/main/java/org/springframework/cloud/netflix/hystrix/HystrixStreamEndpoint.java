package org.springframework.cloud.netflix.hystrix;

import org.springframework.cloud.netflix.endpoint.ServletWrappingEndpoint;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet;

/**
 * User: spencergibb
 * Date: 4/22/14
 * Time: 3:16 PM
 */
public class HystrixStreamEndpoint extends ServletWrappingEndpoint {

    public HystrixStreamEndpoint() {
        super(HystrixMetricsStreamServlet.class, "hystrixStream", "hystrix.stream", false, true);
    }
}
