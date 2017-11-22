package org.springframework.cloud.netflix.eureka.metadata;

import java.util.Objects;

public class ManagementMetadata {

    private final String healthCheckUrl;
    private final String statusPageUrl;
    private final Integer managementPort;

    public ManagementMetadata(String healthCheckUrl, String statusPageUrl, Integer managementPort) {
        this.healthCheckUrl = healthCheckUrl;
        this.statusPageUrl = statusPageUrl;
        this.managementPort = managementPort;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public String getStatusPageUrl() {
        return statusPageUrl;
    }

    public Integer getManagementPort() {
        return managementPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagementMetadata that = (ManagementMetadata) o;
        return Objects.equals(healthCheckUrl, that.healthCheckUrl) &&
                Objects.equals(statusPageUrl, that.statusPageUrl) &&
                Objects.equals(managementPort, that.managementPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(healthCheckUrl, statusPageUrl, managementPort);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ManagementMetadata{");
        sb.append("healthCheckUrl='").append(healthCheckUrl).append('\'');
        sb.append(", statusPageUrl='").append(statusPageUrl).append('\'');
        sb.append(", managementPort=").append(managementPort);
        sb.append('}');
        return sb.toString();
    }
}
