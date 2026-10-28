package na.eat.marketplace.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpiryJob {

    private final OrderService orders;

    public OrderExpiryJob(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(fixedDelayString = "${eatna.expiry-check-ms:30000}", initialDelay = 10000)
    public void run() {
        orders.expireOverdue();
    }
}
