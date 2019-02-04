package org.springframework.cloud.netflix.turbine;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TurbineController {

	private final TurbineInformationService turbineInformationService;

	public TurbineController(TurbineInformationService turbineInformationService) {
		this.turbineInformationService = turbineInformationService;
	}

	@GetMapping(value = "/clusters", produces = MediaType.APPLICATION_JSON_VALUE)
	public Collection<ClusterInformation> clusters(HttpServletRequest request) {
		return turbineInformationService.getClusterInformations(request);
	}

}
