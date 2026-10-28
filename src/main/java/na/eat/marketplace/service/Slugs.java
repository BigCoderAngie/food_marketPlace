package na.eat.marketplace.service;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {

    private Slugs() {
    }

    public static String of(String text) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("['’]", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "seller" : s;
    }
}
