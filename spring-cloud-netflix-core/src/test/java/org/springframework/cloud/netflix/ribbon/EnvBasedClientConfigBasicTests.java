package org.springframework.cloud.netflix.ribbon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

import com.netflix.client.config.CommonClientConfigKey;

public class EnvBasedClientConfigBasicTests {

	@Test
	public void testDefaultsShouldBeReturnedIfNotSet() {
		MockEnvironment mockEnvironment = new MockEnvironment();
		EnvBasedClientConfig envBasedClientConfig = new EnvBasedClientConfig(
				mockEnvironment);
		envBasedClientConfig.loadProperties("sample");
		assertThat(envBasedClientConfig.getProperty(CommonClientConfigKey.ConnectTimeout))
				.isEqualTo("2000");
		assertThat(envBasedClientConfig
				.getProperty(CommonClientConfigKey.OkToRetryOnAllOperations))
						.isEqualTo("false");
	}

	@Test
	public void testOverriddenValueForNamespaceShouldBeReturnedIfSet() {
		MockEnvironment mockEnvironment = new MockEnvironment();
		mockEnvironment.setProperty("ribbon.ConnectTimeout", "2001");
		mockEnvironment.setProperty("ribbon.OkToRetryOnAllOperations", "true");
		EnvBasedClientConfig envBasedClientConfig = new EnvBasedClientConfig(
				mockEnvironment);
		envBasedClientConfig.loadProperties("sample");
		assertThat(envBasedClientConfig.getProperty(CommonClientConfigKey.ConnectTimeout))
				.isEqualTo("2001");
		assertThat(envBasedClientConfig
				.getProperty(CommonClientConfigKey.OkToRetryOnAllOperations))
						.isEqualTo("true");
	}

	@Test
	public void testSpecificValuesForTheClientShouldBeReturned() {
		MockEnvironment mockEnvironment = new MockEnvironment();
		mockEnvironment.setProperty("ribbon.ConnectTimeout", "2001");
		mockEnvironment.setProperty("sample.ribbon.ConnectTimeout", "3001");
        mockEnvironment.setProperty("sample.ribbon.OkToRetryOnAllOperations", "true");
		EnvBasedClientConfig envBasedClientConfig = new EnvBasedClientConfig(
				mockEnvironment);
		envBasedClientConfig.loadProperties("sample");
		assertThat(envBasedClientConfig.getProperty(CommonClientConfigKey.ConnectTimeout))
				.isEqualTo("3001");
        assertThat(envBasedClientConfig
                .getProperty(CommonClientConfigKey.OkToRetryOnAllOperations))
                .isEqualTo("true");
	}
}
