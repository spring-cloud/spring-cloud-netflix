package io.spring.cloud.netflix.turbine;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.turbine.data.DataFromSingleInstance;
import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.handler.PerformanceCriteria;
import com.netflix.turbine.monitor.MonitorConsole;
import com.netflix.turbine.monitor.cluster.AggregateClusterMonitor;
import com.netflix.turbine.monitor.cluster.ObservationCriteria;
import com.netflix.turbine.monitor.instance.InstanceUrlClosure;

/**
 * Created by sgibb on 7/14/14.
 */
public class SpringClusterMonitor extends AggregateClusterMonitor {

    public SpringClusterMonitor(String name, String clusterName) {
        super(name,
                new ObservationCriteria.ClusterBasedObservationCriteria(clusterName),
                new PerformanceCriteria.AggClusterPerformanceCriteria(clusterName),
                new MonitorConsole<DataFromSingleInstance>(),
                InstanceMonitorDispatcher,
                SpringClusterMonitor.ClusterConfigBasedUrlClosure);
    }

    /**
     * TODO: make this a template of some kind (secure, management port, etc...)
     * Helper class that decides how to connect to a server based on injected config.
     * Note that the cluster name must be provided here since one can have different configs for different clusters
     */
    public static InstanceUrlClosure ClusterConfigBasedUrlClosure = new InstanceUrlClosure() {

        private final DynamicStringProperty defaultUrlClosureConfig = DynamicPropertyFactory.getInstance().getStringProperty("turbine.instanceUrlSuffix", null);
        private final DynamicBooleanProperty instanceInsertPort = DynamicPropertyFactory.getInstance().getBooleanProperty("turbine.instanceInsertPort", false);
        @Override
        public String getUrlPath(Instance host) {

            if (host.getCluster() == null) {
                throw new RuntimeException("Host must have cluster name in order to use ClusterConfigBasedUrlClosure");
            }

            String key = "turbine.instanceUrlSuffix." + host.getCluster();
            DynamicStringProperty urlClosureConfig = DynamicPropertyFactory.getInstance().getStringProperty(key, null);

            String url = urlClosureConfig.get();
            if (url == null) {
                url = defaultUrlClosureConfig.get();
            }

            if (url == null) {
                throw new RuntimeException("Config property: " + urlClosureConfig.getName() + " or " +
                        defaultUrlClosureConfig.getName() + " must be set");
            }

            String insertPortKey = "turbine.instanceInsertPort." + host.getCluster();
            DynamicStringProperty insertPortProp = DynamicPropertyFactory.getInstance().getStringProperty(insertPortKey, null);
            boolean insertPort;
            if (insertPortProp.get() == null) {
                insertPort = instanceInsertPort.get();
            } else {
                insertPort = Boolean.parseBoolean(insertPortProp.get());
            }

            if (insertPort) {
                if (url.startsWith("/")) {
                    url = url.substring(1);
                }
                if (!host.getAttributes().containsKey("port")) {
                    throw new RuntimeException("Configured to use port, but port is not in host attributes");
                }
                return String.format("http://%s:%s/%s", host.getHostname(), host.getAttributes().get("port"), url);
            }

            return "http://" + host.getHostname() + url;
        }
    };
}
