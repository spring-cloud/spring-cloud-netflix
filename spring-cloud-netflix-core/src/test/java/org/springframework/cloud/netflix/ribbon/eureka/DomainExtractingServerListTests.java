package org.springframework.cloud.netflix.ribbon.eureka;

import com.google.common.collect.ImmutableMap;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Spencer Gibb
 */
public class DomainExtractingServerListTests {

    static final String IP_ADDR = "10.0.0.2";
    static final int PORT = 8080;
    static final String ZONE = "myzone.mydomain.com";
    static final String HOST_NAME = "myHostName."+ZONE;
    static final String INSTANCE_ID = "myInstanceId";

    @Test
    public void testDomainExtractingServer() {
        DomainExtractingServerList serverList = getDomainExtractingServerList(new DefaultClientConfigImpl());

        List<Server> servers = serverList.getInitialListOfServers();
        assertNotNull("servers was null", servers);
        assertEquals("servers was not size 1", 1, servers.size());

        DomainExtractingServer des = assertDomainExtractingServer(servers);
        assertEquals("hostPort was wrong", HOST_NAME+":"+PORT, des.getHostPort());
    }

    protected DomainExtractingServer assertDomainExtractingServer(List<Server> servers) {
        Server actualServer = servers.get(0);
        assertTrue("server was not a DomainExtractingServer", actualServer instanceof DomainExtractingServer);
        DomainExtractingServer des = DomainExtractingServer.class.cast(actualServer);
        assertEquals("zone was wrong", ZONE, des.getZone());
        assertEquals("instanceId was wrong", INSTANCE_ID, des.getId());
        return des;
    }

    @Test
    public void testDomainExtractingServerUseIpAddress() {
        DefaultClientConfigImpl config = new DefaultClientConfigImpl();
        config.setProperty(CommonClientConfigKey.UseIPAddrForServer, true);
        DomainExtractingServerList serverList = getDomainExtractingServerList(config);

        List<Server> servers = serverList.getInitialListOfServers();
        assertNotNull("servers was null", servers);
        assertEquals("servers was not size 1", 1, servers.size());

        DomainExtractingServer des = assertDomainExtractingServer(servers);
        assertEquals("hostPort was wrong", IP_ADDR+":"+PORT, des.getHostPort());
    }

    protected DomainExtractingServerList getDomainExtractingServerList(DefaultClientConfigImpl config) {
        DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
        ServerList originalServerList = mock(ServerList.class);
        InstanceInfo instanceInfo = mock(InstanceInfo.class);

        when(server.getInstanceInfo()).thenReturn(instanceInfo);
        when(server.getHost()).thenReturn(HOST_NAME);

        when(instanceInfo.getMetadata()).thenReturn(ImmutableMap.<String, String>builder().put("instanceId", INSTANCE_ID).build());
        when(instanceInfo.getHostName()).thenReturn(HOST_NAME);
        when(instanceInfo.getIPAddr()).thenReturn(IP_ADDR);
        when(instanceInfo.getPort()).thenReturn(PORT);

        when(originalServerList.getInitialListOfServers()).thenReturn(Arrays.asList(server));

        return new DomainExtractingServerList(originalServerList, config);
    }

}
