package org.springframework.cloud.netflix.sidecar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Spencer Gibb
 */
@RestController
public class SidecarController {

    @Autowired
    DiscoveryClient discovery;

    @Value("${spring.application.name}")
    String appName;

    @RequestMapping("/ping")
    public String ping() {
        return "OK";
    }

	@RequestMapping("/hosts/{appName}")
	public List<ServiceInstance> hosts(@PathVariable("appName") String appName) {
		return hosts2(appName);
	}

    @RequestMapping("/hosts")
    public List<ServiceInstance> hosts2(@RequestParam("appName") String appName) {
        List<ServiceInstance> instances = discovery.getInstances(appName);
        return instances;
    }

    @RequestMapping(value = "/", produces = "text/html")
    public String home() {
        return "<head><title>Sidecar</title></head><body>\n" +
                "<a href='/ping'>ping</a><br/>\n" +
                "<a href='/health'>health</a><br/>\n" +
                "<a href='/hosts/"+appName+"'>hosts/"+appName+"</a><br/>\n" +
                "</body>";
    }
}
