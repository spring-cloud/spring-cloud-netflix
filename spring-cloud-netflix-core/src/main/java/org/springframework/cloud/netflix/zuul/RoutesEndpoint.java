package org.springframework.cloud.netflix.zuul;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Endpoint to display and reset the zuul proxy routes
 * 
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class RoutesEndpoint implements MvcEndpoint {

	private ZuulHandlerMapping handlerMapping;

	@Autowired
	public RoutesEndpoint(ZuulHandlerMapping handlerMapping) {
		this.handlerMapping = handlerMapping;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Map<String, String> reset() {
		return handlerMapping.reset();
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Map<String, String> getRoutes() {
		return handlerMapping.getRoutes();
	}

	@Override
	public String getPath() {
		return "/routes";
	}

	@Override
	public boolean isSensitive() {
		return true;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}
