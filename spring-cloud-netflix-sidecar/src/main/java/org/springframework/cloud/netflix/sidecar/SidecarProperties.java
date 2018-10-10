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

package org.springframework.cloud.netflix.sidecar;

import java.net.URI;
import java.util.Objects;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 * @author Gregor Zurowski
 * @author Fabrizio Di Napoli
 */
@ConfigurationProperties("sidecar")
public class SidecarProperties {

	private URI healthUri;

	private URI homePageUri;

	@Max(65535)
	@Min(1)
	private int port;

	private String hostname;

	private String ipAddress;

	private boolean acceptAllSslCertificates;

	public URI getHealthUri() {
		return healthUri;
	}

	public void setHealthUri(URI healthUri) {
		this.healthUri = healthUri;
	}

	public URI getHomePageUri() {
		return homePageUri;
	}

	public void setHomePageUri(URI homePageUri) {
		this.homePageUri = homePageUri;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public boolean acceptAllSslCertificates() {
		return acceptAllSslCertificates;
	}

	public void setAcceptAllSslCertificates(boolean acceptAllSslCertificates) {
		this.acceptAllSslCertificates = acceptAllSslCertificates;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SidecarProperties that = (SidecarProperties) o;
		return Objects.equals(healthUri, that.healthUri) &&
				Objects.equals(homePageUri, that.homePageUri) &&
				port == that.port &&
				Objects.equals(hostname, that.hostname) &&
				Objects.equals(ipAddress, that.ipAddress) &&
				Objects.equals(acceptAllSslCertificates, that.acceptAllSslCertificates);
	}

	@Override
	public int hashCode() {
		return Objects.hash(healthUri, homePageUri, port, hostname, ipAddress, acceptAllSslCertificates);
	}

	@Override
	public String toString() {
		return new StringBuilder("SidecarProperties{")
				.append("healthUri=").append(healthUri).append(", ")
				.append("homePageUri=").append(homePageUri).append(", ")
				.append("port=").append(port).append(", ")
				.append("hostname='").append(hostname).append("', ")
				.append("ipAddress='").append(ipAddress).append("', ")
				.append("acceptAllSslCertificates='").append(acceptAllSslCertificates).append("'}")
				.toString();
	}

}
