/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Pair;
import com.netflix.eureka.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.cluster.PeerEurekaNode;
import com.netflix.eureka.resources.StatusResource;
import com.netflix.eureka.util.StatusInfo;

/**
 * @author Spencer Gibb
 */
@Controller
@RequestMapping("${eureka.dashboard.path:/}")
public class EurekaController {

	@Value("${eureka.dashboard.path:/}")
	private String dashboardPath = "";
	private ApplicationInfoManager applicationInfoManager;

	public EurekaController(ApplicationInfoManager applicationInfoManager) {
		this.applicationInfoManager = applicationInfoManager;
	}

	@RequestMapping(method = RequestMethod.GET)
	public String status(HttpServletRequest request, Map<String, Object> model) {
		populateBase(request, model);
		populateApps(model);
		StatusInfo statusInfo;
		try {
			statusInfo = new StatusResource().getStatusInfo();
		}
		catch (Exception e) {
			statusInfo = StatusInfo.Builder.newBuilder().isHealthy(false).build();
		}
		model.put("statusInfo", statusInfo);
		populateInstanceInfo(model, statusInfo);
		return "eureka/status";
	}

	@RequestMapping(value = "/lastn", method = RequestMethod.GET)
	public String lastn(HttpServletRequest request, Map<String, Object> model) {
		populateBase(request, model);
		PeerAwareInstanceRegistryImpl registery = PeerAwareInstanceRegistryImpl.getInstance();
		ArrayList<Map<String, Object>> lastNCanceled = new ArrayList<>();
		List<Pair<Long, String>> list = registery.getLastNCanceledInstances();
		for (Pair<Long, String> entry : list) {
			lastNCanceled.add(registeredInstance(entry.second(), entry.first()
					.longValue()));
		}
		model.put("lastNCanceled", lastNCanceled);
		list = registery.getLastNRegisteredInstances();
		ArrayList<Map<String, Object>> lastNRegistered = new ArrayList<>();
		for (Pair<Long, String> entry : list) {
			lastNRegistered.add(registeredInstance(entry.second(), entry.first()
					.longValue()));
		}
		model.put("lastNRegistered", lastNRegistered);
		return "eureka/lastn";
	}

	private Map<String, Object> registeredInstance(String id, long date) {
		HashMap<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("date", new Date(date));
		return map;
	}

	protected void populateBase(HttpServletRequest request, Map<String, Object> model) {
		model.put("time", new Date());
		model.put("basePath", "/");
		model.put("dashboardPath", this.dashboardPath.equals("/") ? ""
				: this.dashboardPath);
		populateHeader(model);
		populateNavbar(request, model);
	}

	private void populateHeader(Map<String, Object> model) {
		model.put("currentTime", StatusResource.getCurrentTimeAsString());
		model.put("upTime", StatusInfo.getUpTime());
		model.put("environment", ConfigurationManager.getDeploymentContext()
				.getDeploymentEnvironment());
		model.put("datacenter", ConfigurationManager.getDeploymentContext()
				.getDeploymentDatacenter());
		model.put("registry", PeerAwareInstanceRegistryImpl.getInstance());
		model.put("isBelowRenewThresold", PeerAwareInstanceRegistryImpl.getInstance()
				.isBelowRenewThresold() == 1);
		DataCenterInfo info = applicationInfoManager.getInfo().getDataCenterInfo();
		if (info.getName() == DataCenterInfo.Name.Amazon) {
			AmazonInfo amazonInfo = (AmazonInfo) info;
			model.put("amazonInfo", amazonInfo);
			model.put("amiId", amazonInfo.get(AmazonInfo.MetaDataKey.amiId));
			model.put("availabilityZone",
					amazonInfo.get(AmazonInfo.MetaDataKey.availabilityZone));
			model.put("instanceId", amazonInfo.get(AmazonInfo.MetaDataKey.instanceId));
		}
	}

	private void populateNavbar(HttpServletRequest request, Map<String, Object> model) {
		Map<String, String> replicas = new LinkedHashMap<>();
		List<PeerEurekaNode> list = PeerAwareInstanceRegistryImpl.getInstance()
				.getReplicaNodes();
		for (PeerEurekaNode node : list) {
			try {
				URI uri = new URI(node.getServiceUrl());
				String href = node.getServiceUrl();
				replicas.put(uri.getHost(), href);
			}
			catch (Exception ex) {
				// ignore?
			}
		}
		model.put("replicas", replicas.entrySet());
	}

	private void populateApps(Map<String, Object> model) {
		List<com.netflix.discovery.shared.Application> sortedApplications = PeerAwareInstanceRegistryImpl
				.getInstance().getSortedApplications();
		ArrayList<Map<String, Object>> apps = new ArrayList<>();
		for (Application app : sortedApplications) {
			LinkedHashMap<String, Object> appData = new LinkedHashMap<>();
			apps.add(appData);
			appData.put("name", app.getName());
			Map<String, Integer> amiCounts = new HashMap<>();
			Map<InstanceInfo.InstanceStatus, List<Pair<String, String>>> instancesByStatus = new HashMap<>();
			Map<String, Integer> zoneCounts = new HashMap<>();
			for (InstanceInfo info : app.getInstances()) {
				String id = info.getId();
				String url = info.getStatusPageUrl();
				InstanceInfo.InstanceStatus status = info.getStatus();
				String ami = "n/a";
				String zone = "";
				if (info.getDataCenterInfo().getName() == DataCenterInfo.Name.Amazon) {
					AmazonInfo dcInfo = (AmazonInfo) info.getDataCenterInfo();
					ami = dcInfo.get(AmazonInfo.MetaDataKey.amiId);
					zone = dcInfo.get(AmazonInfo.MetaDataKey.availabilityZone);
				}
				Integer count = amiCounts.get(ami);
				if (count != null) {
					amiCounts.put(ami, Integer.valueOf(count.intValue() + 1));
				}
				else {
					amiCounts.put(ami, Integer.valueOf(1));
				}
				count = zoneCounts.get(zone);
				if (count != null) {
					zoneCounts.put(zone, Integer.valueOf(count.intValue() + 1));
				}
				else {
					zoneCounts.put(zone, Integer.valueOf(1));
				}
				List<Pair<String, String>> list = instancesByStatus.get(status);
				if (list == null) {
					list = new ArrayList<>();
					instancesByStatus.put(status, list);
				}
				list.add(new Pair<>(id, url));
			}
			appData.put("amiCounts", amiCounts.entrySet());
			appData.put("zoneCounts", zoneCounts.entrySet());
			ArrayList<Map<String, Object>> instanceInfos = new ArrayList<>();
			appData.put("instanceInfos", instanceInfos);
			for (Iterator<Map.Entry<InstanceInfo.InstanceStatus, List<Pair<String, String>>>> iter = instancesByStatus
					.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<InstanceInfo.InstanceStatus, List<Pair<String, String>>> entry = iter
						.next();
				List<Pair<String, String>> value = entry.getValue();
				InstanceInfo.InstanceStatus status = entry.getKey();
				LinkedHashMap<String, Object> instanceData = new LinkedHashMap<>();
				instanceInfos.add(instanceData);
				instanceData.put("status", entry.getKey());
				ArrayList<Map<String, Object>> instances = new ArrayList<>();
				instanceData.put("instances", instances);
				instanceData.put("isNotUp", status != InstanceInfo.InstanceStatus.UP);

				// TODO

				/*
				 * if(status != InstanceInfo.InstanceStatus.UP){
				 * buf.append("<font color=red size=+1><b>"); }
				 * buf.append("<b>").append(status
				 * .name()).append("</b> (").append(value.size()).append(") - ");
				 * if(status != InstanceInfo.InstanceStatus.UP){
				 * buf.append("</font></b>"); }
				 */

				for (Pair<String, String> p : value) {
					LinkedHashMap<String, Object> instance = new LinkedHashMap<>();
					instances.add(instance);
					instance.put("id", p.first());
					instance.put("url", p.second());
					instance.put("isHref", p.second().startsWith("http"));
					/*
					 * String id = p.first(); String url = p.second(); if(url != null &&
					 * url.startsWith("http")){
					 * buf.append("<a href=\"").append(url).append("\">"); }else { url =
					 * null; } buf.append(id); if(url != null){ buf.append("</a>"); }
					 * buf.append(", ");
					 */
				}
			}
			// out.println("<td>" + buf.toString() + "</td></tr>");
		}
		model.put("apps", apps);
	}

	private void populateInstanceInfo(Map<String, Object> model, StatusInfo statusInfo) {
		InstanceInfo instanceInfo = statusInfo.getInstanceInfo();
		Map<String, String> instanceMap = new HashMap<>();
		instanceMap.put("ipAddr", instanceInfo.getIPAddr());
		instanceMap.put("status", instanceInfo.getStatus().toString());
		if (instanceInfo.getDataCenterInfo().getName() == DataCenterInfo.Name.Amazon) {
			AmazonInfo info = (AmazonInfo) instanceInfo.getDataCenterInfo();
			instanceMap.put("availability-zone",
					info.get(AmazonInfo.MetaDataKey.availabilityZone));
			instanceMap.put("public-ipv4", info.get(AmazonInfo.MetaDataKey.publicIpv4));
			instanceMap.put("instance-id", info.get(AmazonInfo.MetaDataKey.instanceId));
			instanceMap.put("public-hostname",
					info.get(AmazonInfo.MetaDataKey.publicHostname));
			instanceMap.put("ami-id", info.get(AmazonInfo.MetaDataKey.amiId));
			instanceMap.put("instance-type",
					info.get(AmazonInfo.MetaDataKey.instanceType));
		}
		model.put("instanceInfo", instanceMap);
	}
}
