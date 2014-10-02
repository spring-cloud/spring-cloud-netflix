package org.springframework.cloud.netflix.ribbon;

/**
 * @author Spencer Gibb
 */
public interface ServerListInitializer {
    public void initialize(String serviceId);
}
