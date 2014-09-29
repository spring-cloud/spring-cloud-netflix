package org.springframework.cloud.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("spring.cloud.client")
public class ClientProperties {
    private List<String> serviceIds;
}
