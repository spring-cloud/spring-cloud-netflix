package org.springframework.cloud.netflix.ribbon;

import java.util.concurrent.TimeUnit;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Stopwatch;

/**
 * @author Spencer Gibb
 */
public class RibbonStatsRecorder {

	private RibbonLoadBalancerContext context;
	private ServerStats serverStats;
	private Stopwatch tracer;

	public RibbonStatsRecorder(RibbonLoadBalancerContext context, Server server) {
		this.context = context;
		if (server != null) {
			serverStats = context.getServerStats(server);
			context.noteOpenConnection(serverStats);
			tracer = context.getExecuteTracer().start();
		}
	}

	public void recordStats(Object entity) {
		this.recordStats(entity, null);
	}

	public void recordStats(Throwable t) {
		this.recordStats(null, t);
	}

	protected void recordStats(Object entity, Throwable exception) {
		if (this.tracer != null && this.serverStats != null) {
			this.tracer.stop();
			long duration = this.tracer.getDuration(TimeUnit.MILLISECONDS);
			this.context.noteRequestCompletion(serverStats, entity, exception, duration, null/* errorHandler */);
		}
	}
}
