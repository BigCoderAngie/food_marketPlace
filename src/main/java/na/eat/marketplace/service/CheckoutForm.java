package na.eat.marketplace.service;

import lombok.Getter;
import lombok.Setter;
import na.eat.marketplace.domain.FulfilmentMethod;
import na.eat.marketplace.domain.PaymentMethod;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class CheckoutForm {
    private FulfilmentMethod fulfilment = FulfilmentMethod.PICKUP;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime pickupTime;
    private String deliverySuburb;
    private String deliveryLandmark;
    private String contactPhone;
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private String note;
}
