package com.finsphere.chit.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * chit-finance settings ({@code finsphere.chit.*}).
 *
 * @param businessTimeZone time zone that defines "today" for business dates such as a loan's disbursal date
 */
@ConfigurationProperties("finsphere.chit")
public record ChitProperties(@DefaultValue("Asia/Kolkata") String businessTimeZone) {

    public ChitProperties {
        ZoneId.of(businessTimeZone);
    }

    public ZoneId zone() {
        return ZoneId.of(this.businessTimeZone);
    }
}
