package org.springframework.cloud.netflix.zuul.sample;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ZuulProxyApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
public class ZuulProxyApplicationTests {

    @Test
	public void contextLoads() {
	}

}
