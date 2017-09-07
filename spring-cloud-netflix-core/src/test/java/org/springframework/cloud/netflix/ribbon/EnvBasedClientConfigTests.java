package org.springframework.cloud.netflix.ribbon;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.client.config.IClientConfigKey;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

import com.netflix.client.config.CommonClientConfigKey;

public class EnvBasedClientConfigTests {

	@Test
	public void defaultsShouldBeReturnedIfOverridesNotSet() {
		MockEnvironment mockEnvironment = new MockEnvironment();
		EnvBasedClientConfig envBasedClientConfig = new EnvBasedClientConfig(
				mockEnvironment);
		envBasedClientConfig.loadProperties("sample");
		assertThat(envBasedClientConfig.getProperty(CommonClientConfigKey.ConnectTimeout))
				.isEqualTo(2000);
		assertThat(envBasedClientConfig
				.getProperty(CommonClientConfigKey.OkToRetryOnAllOperations))
						.isEqualTo(false);
	}

	@Test
	public void overriddenValueForNamespaceShouldBeReturnedIfSet() {
		MockEnvironment mockEnvironment = new MockEnvironment();
		mockEnvironment.setProperty("ribbon.ConnectTimeout", "2001");
		mockEnvironment.setProperty("ribbon.OkToRetryOnAllOperations", "true");
		EnvBasedClientConfig envBasedClientConfig = new EnvBasedClientConfig(
				mockEnvironment);
		envBasedClientConfig.loadProperties("sample");
		assertThat(envBasedClientConfig.getProperty(CommonClientConfigKey.ConnectTimeout))
				.isEqualTo(2001);
		assertThat(envBasedClientConfig
				.getProperty(CommonClientConfigKey.OkToRetryOnAllOperations))
						.isEqualTo(true);
	}

	@Test
	public void specificValuesForTheClientShouldBeReturned() {
		MockEnvironment mockEnvironment = new MockEnvironment();
		mockEnvironment.setProperty("ribbon.ConnectTimeout", "2001");
		mockEnvironment.setProperty("sample.ribbon.ConnectTimeout", "3001");
		mockEnvironment.setProperty("sample.ribbon.OkToRetryOnAllOperations", "true");
		EnvBasedClientConfig envBasedClientConfig = new EnvBasedClientConfig(
				mockEnvironment);
		envBasedClientConfig.loadProperties("sample");
		assertThat(envBasedClientConfig.getProperty(CommonClientConfigKey.ConnectTimeout))
				.isEqualTo(3001);
		assertThat(envBasedClientConfig
				.getProperty(CommonClientConfigKey.OkToRetryOnAllOperations))
						.isEqualTo(true);
	}

	@Test
	public void proxyHostShouldBeNullByDefault() {
		MockEnvironment mockEnvironment = new MockEnvironment();
		EnvBasedClientConfig envBasedClientConfig = new EnvBasedClientConfig(
				mockEnvironment);
		envBasedClientConfig.loadProperties("sample");
		assertThat(envBasedClientConfig.getProperty(CommonClientConfigKey.ProxyHost))
				.isNull();
	}
	
	@Test
	public void shouldBeAbleToSetAndRetrieveNewProperty() {
		EnvBasedClientConfig config = new EnvBasedClientConfig(new MockEnvironment());
		IClientConfigKey<String> key1 = new CommonClientConfigKey<String>("some-string-prop"){};
		IClientConfigKey<Integer> key2 = new CommonClientConfigKey<Integer>("some-int-prop"){};
		config.setProperty(key1, "someval");
		config.setProperty(key2, 201);
		assertThat(config.getProperty(key1)).isEqualTo("someval");
		assertThat(config.getProperty(key2)).isEqualTo(201);
	}
}
