package org.springframework.netflix.hystrix.amqp;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

/**
 * @author Spencer Gibb
 */
@MessagingGateway
public interface HystrixStreamChannel {

    @Gateway(requestChannel = "hystrixStream")
    public void send(String s);
}
