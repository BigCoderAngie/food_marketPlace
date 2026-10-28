package na.eat.marketplace.domain;

import org.junit.jupiter.api.Test;

import static na.eat.marketplace.domain.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void happyPathForPickupAndDelivery() {
        assertThat(ORDER_PLACED.canMoveTo(ACCEPTED)).isTrue();
        assertThat(ACCEPTED.canMoveTo(PREPARING)).isTrue();
        assertThat(PREPARING.canMoveTo(READY)).isTrue();
        assertThat(READY.canMoveTo(PICKED_UP)).isTrue();
        assertThat(READY.canMoveTo(OUT_FOR_DELIVERY)).isTrue();
        assertThat(OUT_FOR_DELIVERY.canMoveTo(DELIVERED)).isTrue();
        assertThat(PICKED_UP.canMoveTo(COMPLETED)).isTrue();
        assertThat(DELIVERED.canMoveTo(COMPLETED)).isTrue();
    }

    @Test
    void cannotSkipSteps() {
        assertThat(ORDER_PLACED.canMoveTo(READY)).isFalse();
        assertThat(ORDER_PLACED.canMoveTo(COMPLETED)).isFalse();
        assertThat(ACCEPTED.canMoveTo(READY)).isFalse();
        assertThat(OUT_FOR_DELIVERY.canMoveTo(CANCELLED)).isFalse();
    }

    @Test
    void endStatesAreFinal() {
        for (OrderStatus s : new OrderStatus[]{COMPLETED, REJECTED, EXPIRED, CANCELLED}) {
            assertThat(s.isFinal()).as(s.name()).isTrue();
            for (OrderStatus t : values()) assertThat(s.canMoveTo(t)).isFalse();
        }
    }

    @Test
    void onlyAnUnansweredOrderCanBeRejectedOrExpire() {
        for (OrderStatus s : values()) {
            assertThat(s.canMoveTo(REJECTED)).as(s.name()).isEqualTo(s == ORDER_PLACED);
            assertThat(s.canMoveTo(EXPIRED)).as(s.name()).isEqualTo(s == ORDER_PLACED);
        }
    }
}
