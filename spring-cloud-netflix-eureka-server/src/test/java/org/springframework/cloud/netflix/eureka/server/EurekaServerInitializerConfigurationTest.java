package org.springframework.cloud.netflix.eureka.server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EurekaServerInitializerConfigurationTest
{
	@Mock
	private EurekaServerBootstrap eurekaServerBootstrapMock;

	@InjectMocks
	private EurekaServerInitializerConfiguration eurekaServerInitializerConfiguration;

	private boolean callbackCalled;

	@Before
	public void setUp() {
		callbackCalled = false;
	}

	@Test
	public void testStopWithCallbackCallsStop() {
		eurekaServerInitializerConfiguration.stop(this::setCallbackCalledTrue);

		assertThat(callbackCalled).isTrue();
		verify(eurekaServerBootstrapMock).contextDestroyed(any());
	}

	private void setCallbackCalledTrue() {
		callbackCalled = true;
	}
}