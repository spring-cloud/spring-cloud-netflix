package org.springframework.cloud.netflix.sidecar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SidecarApplication.class)
@IntegrationTest("server.port=0")
public class SidecarApplicationTests {

	@Test
	public void contextLoads() {
	}

}
