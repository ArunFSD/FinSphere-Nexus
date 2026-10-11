package com.finsphere.frontend.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Front-end settings ({@code finsphere.frontend.*}).
 *
 * @param timeZone       zone used to show dates and times to people
 * @param supportContact who to contact for help, shown on the password help page
 * @param pageSize       rows per page in lists
 */
@ConfigurationProperties("finsphere.frontend")
public record FrontendProperties(
        @DefaultValue("Asia/Kolkata") String timeZone,
        @DefaultValue("your branch office") String supportContact,
        @DefaultValue("20") int pageSize) {

    public FrontendProperties {
        ZoneId.of(timeZone);
        if (pageSize < 5 || pageSize > 100) {
            throw new IllegalArgumentException("finsphere.frontend.page-size must be between 5 and 100");
        }
    }

    public ZoneId zone() {
        return ZoneId.of(this.timeZone);
    }
}
