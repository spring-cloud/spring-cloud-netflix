package org.springframework.cloud.netflix.sidecar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

/**
 * @author Spencer Gibb
 */
public class LocalApplicationHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    SidecarProperties properties;

    @SuppressWarnings("unchecked")
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        URI uri = properties.getHealthUri();
        if (uri == null) {
            builder.up();
            return;
        }

        Map<String, Object> map = new RestTemplate().getForObject(uri, Map.class);
        Object status = map.get("status");

        if (status != null && status instanceof String) {
            builder.status(status.toString());
        } else if (status != null && status instanceof Map) {
            Map<String, Object> statusMap = (Map<String, Object>) status;
            Object code = statusMap.get("code");
            if (code != null) {
                builder.status(code.toString());
            } else {
                getWarning(builder);
            }
        } else {
            getWarning(builder);
        }
    }

    private Health.Builder getWarning(Health.Builder builder) {
        return builder.unknown().withDetail("warning", "no status field in response");
    }
}
