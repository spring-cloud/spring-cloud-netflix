package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.RetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerContext;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Timer;

/**
 * @author Spencer Gibb
 */
public class RibbonLoadBalancerContext extends LoadBalancerContext {
    public RibbonLoadBalancerContext(ILoadBalancer lb) {
        super(lb);
    }

    public RibbonLoadBalancerContext(ILoadBalancer lb, IClientConfig clientConfig) {
        super(lb, clientConfig);
    }

    public RibbonLoadBalancerContext(ILoadBalancer lb, IClientConfig clientConfig, RetryHandler handler) {
        super(lb, clientConfig, handler);
    }

    @Override
    public void noteOpenConnection(ServerStats serverStats) {
        super.noteOpenConnection(serverStats);
    }

    @Override
    public Timer getExecuteTracer() {
        return super.getExecuteTracer();
    }

    @Override
    public void noteRequestCompletion(ServerStats stats, Object response, Throwable e, long responseTime) {
        super.noteRequestCompletion(stats, response, e, responseTime);
    }

    @Override
    public void noteRequestCompletion(ServerStats stats, Object response, Throwable e, long responseTime, RetryHandler errorHandler) {
        super.noteRequestCompletion(stats, response, e, responseTime, errorHandler);
    }
}
