/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.http.oauth2;

import org.springframework.cloud.netflix.eureka.http.EurekaRestTemplateFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * @author Daniel Lavoie
 */
public class OAuth2EurekaRestTemplateFactory implements EurekaRestTemplateFactory {
	private final EurekaOAuth2ResourceDetails eurekaOAuth2ResourceDetails;

	public OAuth2EurekaRestTemplateFactory(
			EurekaOAuth2ResourceDetails eurekaOAuth2ResourceDetails) {
		this.eurekaOAuth2ResourceDetails = eurekaOAuth2ResourceDetails;
	}

	@Override
	public RestTemplate newRestTemplate(String serviceUrl) {
		return new OAuth2RestTemplate(eurekaOAuth2ResourceDetails);
	}
}
