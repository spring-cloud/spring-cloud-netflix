package org.springframework.cloud.netflix.turbine;

import static org.junit.Assert.*;

import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;

/**
 * @author Spencer Gibb
 */
public class EurekaInstanceDiscoveryTest {


    @Test
    public void testGetClusterName() {
        String appName = "testAppName";
        EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(new TurbineProperties());
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                .setAppName(appName)
                .build();
        String clusterName = discovery.getClusterName(instanceInfo);
        assertEquals("clusterName is wrong", appName.toUpperCase(), clusterName);
    }

    @Test
    public void testGetClusterNameCustomExpression() {
        TurbineProperties turbineProperties = new TurbineProperties();
        turbineProperties.setClusterNameExpression("aSGName");
        EurekaInstanceDiscovery discovery = new EurekaInstanceDiscovery(turbineProperties);
        String asgName = "myAsgName";
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                .setAppName("testApp")
                .setASGName(asgName)
                .build();
        String clusterName = discovery.getClusterName(instanceInfo);
        assertEquals("clusterName is wrong", asgName, clusterName);
    }
}
