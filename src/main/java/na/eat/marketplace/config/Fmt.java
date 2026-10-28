package na.eat.marketplace.config;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting helpers for templates: ${@fmt.money(price)}. Prices are in N$ (SRS 2.3). */
@Component("fmt")
public class Fmt {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private final Clock clock;

    public Fmt(Clock clock) {
        this.clock = clock;
    }

    public String money(BigDecimal amount) {
        if (amount == null) return "";
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return "N$ " + df.format(amount);
    }

    public String time(Instant instant) {
        return instant == null ? "" : TIME.format(instant.atZone(clock.getZone()));
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(clock.getZone()));
    }

    public String dateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        LocalDate today = LocalDate.now(clock);
        if (dateTime.toLocalDate().equals(today)) return "Today " + TIME.format(dateTime);
        if (dateTime.toLocalDate().equals(today.plusDays(1))) return "Tomorrow " + TIME.format(dateTime);
        return DAY.format(dateTime) + " " + TIME.format(dateTime);
    }

    /** Epoch millis for the countdown script. */
    public long epoch(Instant instant) {
        return instant == null ? 0 : instant.toEpochMilli();
    }

    public String ago(Instant instant) {
        if (instant == null) return "";
        Duration d = Duration.between(instant, clock.instant());
        if (d.toMinutes() < 1) return "just now";
        if (d.toMinutes() < 60) return d.toMinutes() + " min ago";
        if (d.toHours() < 24) return d.toHours() + " h ago";
        return DATE_TIME.format(instant.atZone(clock.getZone()));
    }

    public String stars(Number rating) {
        if (rating == null) return "";
        int full = (int) Math.round(rating.doubleValue());
        return "★".repeat(Math.max(0, Math.min(5, full))) + "☆".repeat(Math.max(0, 5 - full));
    }

    public String rating(BigDecimal avg) {
        return avg == null ? "" : avg.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
