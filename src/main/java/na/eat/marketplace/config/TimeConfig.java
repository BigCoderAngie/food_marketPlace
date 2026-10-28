package na.eat.marketplace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    /** All "today" logic (daily portions, reports) uses Namibian time. */
    @Bean
    public Clock clock(@Value("${eatna.timezone:Africa/Windhoek}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
