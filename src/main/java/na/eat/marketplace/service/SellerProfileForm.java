package na.eat.marketplace.service;

import lombok.Getter;
import lombok.Setter;
import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.domain.SellerType;

@Getter
@Setter
public class SellerProfileForm {
    private String name;
    private SellerType type = SellerType.HOME_COOK;
    private String description;
    private String phone;
    private String whatsapp;
    private String town;
    private String suburb;
    private String pickupInstructions;
    private String openingHours;
    private String paymentInstructions;
    private boolean offersPickup;
    private boolean offersDelivery;
    private int acceptWindowMinutes = 10;

    public static SellerProfileForm from(Seller s) {
        SellerProfileForm f = new SellerProfileForm();
        f.name = s.getName();
        f.type = s.getType();
        f.description = s.getDescription();
        f.phone = s.getPhone();
        f.whatsapp = s.getWhatsapp();
        f.town = s.getTown();
        f.suburb = s.getSuburb();
        f.pickupInstructions = s.getPickupInstructions();
        f.openingHours = s.getOpeningHours();
        f.paymentInstructions = s.getPaymentInstructions();
        f.offersPickup = s.isOffersPickup();
        f.offersDelivery = s.isOffersDelivery();
        f.acceptWindowMinutes = s.getAcceptWindowMinutes();
        return f;
    }
}
