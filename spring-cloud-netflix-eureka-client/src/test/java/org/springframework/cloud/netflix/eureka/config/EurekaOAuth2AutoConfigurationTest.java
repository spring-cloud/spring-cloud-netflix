package org.springframework.cloud.netflix.eureka.config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.eureka.http.oauth2.EurekaOAuth2ResourceDetails;
import org.springframework.cloud.netflix.eureka.http.oauth2.OAuth2EurekaRestTemplateFactory;
import org.springframework.cloud.netflix.eureka.sample.EurekaSampleApplication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EurekaSampleApplication.class, properties = {
		"spring.profiles.active=oauth2" }, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class EurekaOAuth2AutoConfigurationTest {
	@Autowired
	private OAuth2EurekaRestTemplateFactory eurekaRestTemplateFactory;

	@Autowired
	private EurekaOAuth2ResourceDetails resourceDetails;

	@Test
	public void contextLoads() {
		Assert.assertNotNull(eurekaRestTemplateFactory);
		Assert.assertNotNull(resourceDetails);
	}
}
