package na.eat.marketplace.service;

/** Namibian phone numbers, normalised to +264XXXXXXXXX. */
public final class Phones {

    private Phones() {
    }

    public static String normalise(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) return null;
        if (digits.startsWith("00")) digits = "+" + digits.substring(2);
        if (digits.startsWith("0")) digits = "+264" + digits.substring(1);
        if (!digits.startsWith("+")) digits = "+264" + digits;
        return digits;
    }

    public static boolean isValid(String normalised) {
        return normalised != null && normalised.matches("\\+264\\d{8,9}");
    }
}
