package na.eat.marketplace.web.seller;

import na.eat.marketplace.domain.*;

import java.util.ArrayList;
import java.util.List;

/** The buttons a seller sees for an order, derived from its state. */
public record SellerAction(String key, String label, String style, boolean needsReason) {

    public static List<SellerAction> forOrder(CustomerOrder o) {
        List<SellerAction> list = new ArrayList<>();
        boolean pickup = o.getFulfilment() == FulfilmentMethod.PICKUP;
        switch (o.getStatus()) {
            case ORDER_PLACED -> {
                list.add(new SellerAction("accept", "Accept order", "primary", false));
                list.add(new SellerAction("reject", "Reject", "danger", true));
            }
            case ACCEPTED -> {
                list.add(new SellerAction("prepare", "Start preparing", "primary", false));
                list.add(new SellerAction("cancel", "Cancel order", "danger", true));
            }
            case PREPARING -> {
                list.add(new SellerAction("ready", "Mark ready", "primary", false));
                list.add(new SellerAction("cancel", "Cancel order", "danger", true));
            }
            case READY -> {
                list.add(new SellerAction("handover", pickup ? "Customer collected" : "Send out for delivery", "primary", false));
                list.add(new SellerAction("cancel", "Cancel order", "danger", true));
            }
            case OUT_FOR_DELIVERY -> {
                list.add(new SellerAction("delivered", "Mark delivered", "primary", false));
                list.add(new SellerAction("problem", "Report a delivery problem", "danger", true));
            }
            case PICKED_UP, DELIVERED -> list.add(new SellerAction("complete", "Complete order", "primary", false));
            default -> {
            }
        }
        PaymentStatus ps = o.getPayment().getStatus();
        if (!o.getStatus().isFailed() && (ps == PaymentStatus.UNPAID || ps == PaymentStatus.AWAITING_CONFIRMATION)) {
            list.add(new SellerAction("paid", "Confirm payment received", "secondary", false));
        }
        if (ps == PaymentStatus.REFUND_DUE) list.add(new SellerAction("refunded", "Mark refund sent", "secondary", false));
        return list;
    }
}
