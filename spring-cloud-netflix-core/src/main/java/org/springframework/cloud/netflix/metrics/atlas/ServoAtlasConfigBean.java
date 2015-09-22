/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */


package org.springframework.cloud.netflix.metrics.atlas;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.netflix.servo.publish.atlas.ServoAtlasConfig;

/**
 * @author Jon Schneider
 */
@ConfigurationProperties("netflix.atlas")
public class ServoAtlasConfigBean implements ServoAtlasConfig {
    private String uri;
    private Integer pushQueueSize = 1000;
    private boolean enabled = true;
    private Integer batchSize = 10000;

    @Override
    public String getAtlasUri() {
        return uri;
    }

    @Override
    public int getPushQueueSize() {
        return pushQueueSize;
    }

    @Override
    public boolean shouldSendMetrics() {
        return enabled;
    }

    @Override
    public int batchSize() {
        return batchSize;
    }
}
