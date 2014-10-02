package org.springframework.cloud.client;


/**
 * @author Spencer Gibb
 * TODO: name? Server? HostAndPort? Instance?
 */
public interface ServiceInstance {
    public String getHost();
    public int getPort();
}
