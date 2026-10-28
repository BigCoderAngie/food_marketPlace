package na.eat.marketplace.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhonesTest {

    @Test
    void normalisesNamibianNumbers() {
        assertThat(Phones.normalise("081 234 5678")).isEqualTo("+264812345678");
        assertThat(Phones.normalise("+264 81 234 5678")).isEqualTo("+264812345678");
        assertThat(Phones.normalise("00264812345678")).isEqualTo("+264812345678");
        assertThat(Phones.isValid(Phones.normalise("081 234 5678"))).isTrue();
        assertThat(Phones.isValid(Phones.normalise("1234"))).isFalse();
    }

    @Test
    void slugsDropApostrophes() {
        assertThat(Slugs.of("Meme Ndapandula's Kitchen")).isEqualTo("meme-ndapandulas-kitchen");
        assertThat(Slugs.of("Café Brötchen!")).isEqualTo("cafe-brotchen");
    }
}
