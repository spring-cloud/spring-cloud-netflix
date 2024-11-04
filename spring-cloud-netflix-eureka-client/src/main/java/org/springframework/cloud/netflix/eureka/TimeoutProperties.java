package org.springframework.cloud.netflix.eureka;

import java.util.Objects;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.io.SocketConfig;

/**
 * @author Olga Maciaszek-Sharma
 */
public class TimeoutProperties {


	/**
	 * Default values are set to 180000, in keeping with {@link RequestConfig} and
	 * {@link SocketConfig} defaults.
	 */
	protected int connectTimeout = 180000; // 3 * MINUTES

	protected int connectRequestTimeout = 180000; // 3 * MINUTES

	protected int socketTimeout = 180000; // 3 * MINUTES

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public int getConnectRequestTimeout() {
		return connectRequestTimeout;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setConnectRequestTimeout(int connectRequestTimeout) {
		this.connectRequestTimeout = connectRequestTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		RestTemplateTimeoutProperties that = (RestTemplateTimeoutProperties) o;

		return connectTimeout == that.connectTimeout && connectRequestTimeout == that.connectRequestTimeout
				&& socketTimeout == that.socketTimeout;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectTimeout, connectRequestTimeout, socketTimeout);
	}

}
