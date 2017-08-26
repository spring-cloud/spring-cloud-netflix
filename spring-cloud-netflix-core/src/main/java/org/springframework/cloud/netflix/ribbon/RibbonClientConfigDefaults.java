package org.springframework.cloud.netflix.ribbon;

import java.util.concurrent.TimeUnit;

import com.netflix.client.config.DefaultClientConfigImpl;

/**
 * This is shadowing the defaults defined in {@link DefaultClientConfigImpl}, this way the
 * rest of the code can refer to this class over {@link DefaultClientConfigImpl} and any
 * changes to the defaults carry over.
 */

public class RibbonClientConfigDefaults {
	public static final Boolean DEFAULT_PRIORITIZE_VIP_ADDRESS_BASED_SERVERS = DefaultClientConfigImpl.DEFAULT_PRIORITIZE_VIP_ADDRESS_BASED_SERVERS;

	public static final String DEFAULT_NFLOADBALANCER_PING_CLASSNAME = DefaultClientConfigImpl.DEFAULT_NFLOADBALANCER_PING_CLASSNAME;

	public static final String DEFAULT_NFLOADBALANCER_RULE_CLASSNAME = DefaultClientConfigImpl.DEFAULT_NFLOADBALANCER_RULE_CLASSNAME;

	public static final String DEFAULT_NFLOADBALANCER_CLASSNAME = DefaultClientConfigImpl.DEFAULT_NFLOADBALANCER_CLASSNAME;

	public static final boolean DEFAULT_USEIPADDRESS_FOR_SERVER = DefaultClientConfigImpl.DEFAULT_USEIPADDRESS_FOR_SERVER;

	public static final String DEFAULT_CLIENT_CLASSNAME = DefaultClientConfigImpl.DEFAULT_CLIENT_CLASSNAME;

	public static final String DEFAULT_VIPADDRESS_RESOLVER_CLASSNAME = DefaultClientConfigImpl.DEFAULT_VIPADDRESS_RESOLVER_CLASSNAME;

	public static final String DEFAULT_PRIME_CONNECTIONS_URI = DefaultClientConfigImpl.DEFAULT_PRIME_CONNECTIONS_URI;

	public static final int DEFAULT_MAX_TOTAL_TIME_TO_PRIME_CONNECTIONS = DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_TIME_TO_PRIME_CONNECTIONS;

	public static final int DEFAULT_MAX_RETRIES_PER_SERVER_PRIME_CONNECTION = DefaultClientConfigImpl.DEFAULT_MAX_RETRIES_PER_SERVER_PRIME_CONNECTION;

	public static final Boolean DEFAULT_ENABLE_PRIME_CONNECTIONS = DefaultClientConfigImpl.DEFAULT_ENABLE_PRIME_CONNECTIONS;

	public static final Boolean DEFAULT_ENABLE_GZIP_CONTENT_ENCODING_FILTER = DefaultClientConfigImpl.DEFAULT_ENABLE_GZIP_CONTENT_ENCODING_FILTER;

	public static final Boolean DEFAULT_CONNECTION_POOL_CLEANER_TASK_ENABLED = DefaultClientConfigImpl.DEFAULT_CONNECTION_POOL_CLEANER_TASK_ENABLED;

	public static final Boolean DEFAULT_FOLLOW_REDIRECTS = DefaultClientConfigImpl.DEFAULT_FOLLOW_REDIRECTS;

	public static final int DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER = DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER;

	public static final int DEFAULT_MAX_AUTO_RETRIES = DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES;

	public static final int DEFAULT_READ_TIMEOUT = DefaultClientConfigImpl.DEFAULT_READ_TIMEOUT;

	public static final int DEFAULT_CONNECTION_MANAGER_TIMEOUT = DefaultClientConfigImpl.DEFAULT_CONNECTION_MANAGER_TIMEOUT;

	public static final int DEFAULT_CONNECT_TIMEOUT = DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT;

	public static final Boolean DEFAULT_ENABLE_CONNECTION_POOL = DefaultClientConfigImpl.DEFAULT_ENABLE_CONNECTION_POOL;

	@Deprecated
	public static final int DEFAULT_MAX_HTTP_CONNECTIONS_PER_HOST = DefaultClientConfigImpl.DEFAULT_MAX_HTTP_CONNECTIONS_PER_HOST;

	@Deprecated
	public static final int DEFAULT_MAX_TOTAL_HTTP_CONNECTIONS = DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_HTTP_CONNECTIONS;

	public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = DefaultClientConfigImpl.DEFAULT_MAX_CONNECTIONS_PER_HOST;

	public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_CONNECTIONS;

	public static final float DEFAULT_MIN_PRIME_CONNECTIONS_RATIO = DefaultClientConfigImpl.DEFAULT_MIN_PRIME_CONNECTIONS_RATIO;

	public static final String DEFAULT_PRIME_CONNECTIONS_CLASS = DefaultClientConfigImpl.DEFAULT_PRIME_CONNECTIONS_CLASS;

	public static final String DEFAULT_SEVER_LIST_CLASS = DefaultClientConfigImpl.DEFAULT_SEVER_LIST_CLASS;

	public static final int DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS = DefaultClientConfigImpl.DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS;

	public static final int DEFAULT_CONNECTIONIDLE_TIME_IN_MSECS = DefaultClientConfigImpl.DEFAULT_CONNECTIONIDLE_TIME_IN_MSECS;

	public static final int DEFAULT_POOL_MAX_THREADS = DefaultClientConfigImpl.DEFAULT_POOL_MAX_THREADS;

	public static final int DEFAULT_POOL_MIN_THREADS = DefaultClientConfigImpl.DEFAULT_POOL_MIN_THREADS;

	public static final long DEFAULT_POOL_KEEP_ALIVE_TIME = DefaultClientConfigImpl.DEFAULT_POOL_KEEP_ALIVE_TIME;

	public static final TimeUnit DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS = DefaultClientConfigImpl.DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS;

	public static final Boolean DEFAULT_ENABLE_ZONE_AFFINITY = DefaultClientConfigImpl.DEFAULT_ENABLE_ZONE_AFFINITY;

	public static final Boolean DEFAULT_ENABLE_ZONE_EXCLUSIVITY = DefaultClientConfigImpl.DEFAULT_ENABLE_ZONE_EXCLUSIVITY;

	public static final int DEFAULT_PORT = DefaultClientConfigImpl.DEFAULT_PORT;

	public static final String DEFAULT_PROPERTY_NAME_SPACE = DefaultClientConfigImpl.DEFAULT_PROPERTY_NAME_SPACE;

	public static final Boolean DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS = DefaultClientConfigImpl.DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS;

	public static final Boolean DEFAULT_IS_CLIENT_AUTH_REQUIRED = DefaultClientConfigImpl.DEFAULT_IS_CLIENT_AUTH_REQUIRED;

	private RibbonClientConfigDefaults() {
		throw new AssertionError("Must not instantiate constant utility class");
	}
}
