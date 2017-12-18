package org.springframework.cloud.netflix.eureka.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.http.EurekaRestTemplateFactory;
import org.springframework.cloud.netflix.eureka.http.oauth2.EurekaOAuth2ResourceDetails;
import org.springframework.cloud.netflix.eureka.http.oauth2.OAuth2EurekaRestTemplateFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;

@Configuration
@ConditionalOnClass(BaseOAuth2ProtectedResourceDetails.class)
public class EurekaOAuth2AutoConfiguration {
	@Bean
	@ConditionalOnProperty("eureka.client.oauth2.client-id")
	public EurekaOAuth2ResourceDetails eurekaOAuth2ResourceDetails() {
		return new EurekaOAuth2ResourceDetails();
	}

	@Bean
	@ConditionalOnBean(EurekaOAuth2ResourceDetails.class)
	public EurekaRestTemplateFactory eurekaRestTemplateFactory(
			EurekaOAuth2ResourceDetails eurekaOAuth2ResourceDetails) {
		return new OAuth2EurekaRestTemplateFactory(eurekaOAuth2ResourceDetails);
	}
}
