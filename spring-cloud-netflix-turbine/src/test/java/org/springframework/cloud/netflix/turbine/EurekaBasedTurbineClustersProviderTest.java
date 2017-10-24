package org.springframework.cloud.netflix.turbine;

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EurekaBasedTurbineClustersProviderTest {

	EurekaClient eurekaClient = mock(EurekaClient.class);
	TurbineClustersProvider provider = new EurekaBasedTurbineClustersProvider(eurekaClient);

	@Test
	public void shouldProvideAllClustersNames() throws Exception {
		Applications applications = registeredApplications(asList(application("service1"),
				application("service2"), application("service3")));
		when(eurekaClient.getApplications()).thenReturn(applications);

		List<String> clusterNames = provider.getClusterNames();

		assertThat(clusterNames).containsOnly("service1", "service2", "service3");
	}

	private Applications registeredApplications(List<Application> registered) {
		Applications applications = mock(Applications.class);
		when(applications.getRegisteredApplications()).thenReturn(registered);
		return applications;
	}

	private Application application(String name) {
		Application application = mock(Application.class);
		when(application.getName()).thenReturn(name);
		return application;
	}
}