package org.springframework.cloud.netflix.ribbon;

import java.util.concurrent.TimeUnit;

public abstract class RibbonClientConfigDefaults {
	public static final Boolean DEFAULT_PRIORITIZE_VIP_ADDRESS_BASED_SERVERS = Boolean.TRUE;

	public static final String DEFAULT_NFLOADBALANCER_PING_CLASSNAME = "com.netflix.loadbalancer.DummyPing"; // DummyPing.class.getName();

	public static final String DEFAULT_NFLOADBALANCER_RULE_CLASSNAME = "com.netflix.loadbalancer.AvailabilityFilteringRule";

	public static final String DEFAULT_NFLOADBALANCER_CLASSNAME = "com.netflix.loadbalancer.ZoneAwareLoadBalancer";

	public static final boolean DEFAULT_USEIPADDRESS_FOR_SERVER = Boolean.FALSE;

	public static final String DEFAULT_CLIENT_CLASSNAME = "com.netflix.niws.client.http.RestClient";

	public static final String DEFAULT_VIPADDRESS_RESOLVER_CLASSNAME = "com.netflix.client.SimpleVipAddressResolver";

	public static final String DEFAULT_PRIME_CONNECTIONS_URI = "/";

	public static final int DEFAULT_MAX_TOTAL_TIME_TO_PRIME_CONNECTIONS = 30000;

	public static final int DEFAULT_MAX_RETRIES_PER_SERVER_PRIME_CONNECTION = 9;

	public static final Boolean DEFAULT_ENABLE_PRIME_CONNECTIONS = Boolean.FALSE;

	public static final Boolean DEFAULT_ENABLE_GZIP_CONTENT_ENCODING_FILTER = Boolean.FALSE;

	public static final Boolean DEFAULT_CONNECTION_POOL_CLEANER_TASK_ENABLED = Boolean.TRUE;

	public static final Boolean DEFAULT_FOLLOW_REDIRECTS = Boolean.FALSE;

	public static final int DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER = 1;

	public static final int DEFAULT_MAX_AUTO_RETRIES = 0;

	public static final int DEFAULT_READ_TIMEOUT = 5000;

	public static final int DEFAULT_CONNECTION_MANAGER_TIMEOUT = 2000;

	public static final int DEFAULT_CONNECT_TIMEOUT = 2000;

	public static final Boolean DEFAULT_ENABLE_CONNECTION_POOL = Boolean.TRUE;

	@Deprecated
	public static final int DEFAULT_MAX_HTTP_CONNECTIONS_PER_HOST = 50;

	@Deprecated
	public static final int DEFAULT_MAX_TOTAL_HTTP_CONNECTIONS = 200;

	public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 50;

	public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;

	public static final float DEFAULT_MIN_PRIME_CONNECTIONS_RATIO = 1.0f;

	public static final String DEFAULT_PRIME_CONNECTIONS_CLASS = "com.netflix.niws.client.http.HttpPrimeConnection";

	public static final String DEFAULT_SEVER_LIST_CLASS = "com.netflix.loadbalancer.ConfigurationBasedServerList";

	public static final int DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS = 30000;

	public static final int DEFAULT_CONNECTIONIDLE_TIME_IN_MSECS = 30000; 

	public static final int DEFAULT_POOL_MAX_THREADS = DEFAULT_MAX_TOTAL_HTTP_CONNECTIONS;
	public static final int DEFAULT_POOL_MIN_THREADS = 1;
	public static final long DEFAULT_POOL_KEEP_ALIVE_TIME = 15 * 60L;
	public static final TimeUnit DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS = TimeUnit.SECONDS;
	public static final Boolean DEFAULT_ENABLE_ZONE_AFFINITY = Boolean.FALSE;
	public static final Boolean DEFAULT_ENABLE_ZONE_EXCLUSIVITY = Boolean.FALSE;
	public static final int DEFAULT_PORT = 7001;

	public static final String DEFAULT_PROPERTY_NAME_SPACE = "ribbon";

	public static final Boolean DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS = Boolean.FALSE;

	public static final Boolean DEFAULT_IS_CLIENT_AUTH_REQUIRED = Boolean.FALSE;
}
