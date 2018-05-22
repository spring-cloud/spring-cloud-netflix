package org.springframework.cloud.netflix.ribbon;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;

import java.util.concurrent.TimeUnit;

import static com.netflix.client.config.CommonClientConfigKey.PoolKeepAliveTime;
import static com.netflix.client.config.CommonClientConfigKey.PoolKeepAliveTimeUnits;
import static com.netflix.client.config.CommonClientConfigKey.Port;
import static com.netflix.client.config.CommonClientConfigKey.SecurePort;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_CONNECTIONS_PER_HOST;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_CONNECTIONS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_POOL_KEEP_ALIVE_TIME;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_PORT;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_READ_TIMEOUT;

public class RibbonProperties {
    private final IClientConfig config;

    public static RibbonProperties from(IClientConfig config) {
        return new RibbonProperties(config);
    }

    RibbonProperties(IClientConfig config) {
        this.config = config;
    }

    public Integer getConnectionCleanerRepeatInterval() {
        return get(CommonClientConfigKey.ConnectionCleanerRepeatInterval);
    }

    public int connectionCleanerRepeatInterval() {
        return get(CommonClientConfigKey.ConnectionCleanerRepeatInterval,
                DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS);
    }

    public Integer getConnectTimeout() {
        return get(CommonClientConfigKey.ConnectTimeout);
    }

    public int connectTimeout() {
        return connectTimeout(DEFAULT_CONNECT_TIMEOUT);
    }

    public int connectTimeout(int defaultValue) {
        return get(CommonClientConfigKey.ConnectTimeout, defaultValue);
    }

    public Boolean getFollowRedirects() {
        return get(CommonClientConfigKey.FollowRedirects);
    }

    public boolean isFollowRedirects() {
        return isFollowRedirects(DEFAULT_FOLLOW_REDIRECTS);
    }

    public boolean isFollowRedirects(boolean defaultValue) {
        return get(CommonClientConfigKey.FollowRedirects, defaultValue);
    }

    public Integer getMaxConnectionsPerHost() {
        return get(CommonClientConfigKey.MaxConnectionsPerHost);
    }

    public int maxConnectionsPerHost() {
        return maxConnectionsPerHost(DEFAULT_MAX_CONNECTIONS_PER_HOST);
    }

    public int maxConnectionsPerHost(int defaultValue) {
        return get(CommonClientConfigKey.MaxConnectionsPerHost, defaultValue);
    }

    public Integer getMaxTotalConnections() {
        return get(CommonClientConfigKey.MaxTotalConnections);
    }

    public int maxTotalConnections() {
        return maxTotalConnections(DEFAULT_MAX_TOTAL_CONNECTIONS);
    }

    public int maxTotalConnections(int defaultValue) {
        return get(CommonClientConfigKey.MaxTotalConnections, defaultValue);
    }

    public Boolean getOkToRetryOnAllOperations() {
        return get(CommonClientConfigKey.OkToRetryOnAllOperations);
    }

    public boolean isOkToRetryOnAllOperations() {
        return get(CommonClientConfigKey.OkToRetryOnAllOperations,
                DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS);
    }

    @SuppressWarnings("deprecation")
    public Long getPoolKeepAliveTime() {
        Object property = this.config.getProperty(PoolKeepAliveTime);
        if (property instanceof Long) {
            return (Long) property;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public long poolKeepAliveTime() {
        Object property = this.config.getProperty(PoolKeepAliveTime);
        if (property instanceof Long) {
            return (Long) property;
        }
        return DEFAULT_POOL_KEEP_ALIVE_TIME;
    }

    @SuppressWarnings("deprecation")
    public TimeUnit getPoolKeepAliveTimeUnits() {
        Object property = this.config.getProperty(PoolKeepAliveTimeUnits);
        if (property instanceof TimeUnit) {
            return (TimeUnit) property;
        }
        return DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS;
    }

    public Integer getPort() {
        return get(Port);
    }

    public int port() {
        return get(Port, DEFAULT_PORT);
    }

    public Integer getReadTimeout() {
        return get(CommonClientConfigKey.ReadTimeout);
    }

    public int readTimeout() {
        return readTimeout(DEFAULT_READ_TIMEOUT);
    }

    public int readTimeout(int defaultValue) {
        return get(CommonClientConfigKey.ReadTimeout, defaultValue);
    }

    public Boolean getSecure() {
        return get(CommonClientConfigKey.IsSecure);
    }

    public boolean isSecure() {
        return isSecure(false);
    }

    public boolean isSecure(boolean defaultValue) {
        return get(CommonClientConfigKey.IsSecure, defaultValue);
    }

    public Integer getSecurePort() {
        return this.config.get(SecurePort);
    }

    public Boolean getUseIPAddrForServer() {
        return get(CommonClientConfigKey.UseIPAddrForServer);
    }

    public boolean isUseIPAddrForServer() {
        return isUseIPAddrForServer(false);
    }

    public boolean isUseIPAddrForServer(boolean defaultValue) {
        return get(CommonClientConfigKey.UseIPAddrForServer, defaultValue);
    }

    public <T> boolean has(IClientConfigKey<T> key) {
        return this.config.containsProperty(key);
    }

    public <T> T get(IClientConfigKey<T> key) {
        return this.config.get(key);
    }

    public <T> T get(IClientConfigKey<T> key, T defaultValue) {
        return this.config.get(key, defaultValue);
    }
}
