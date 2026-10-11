package com.finsphere.frontend.web.support;

/** Names of the back-end services as people should read them. */
public final class Services {

    private Services() {
    }

    public static String describe(String serviceId) {
        return switch (serviceId) {
            case "auth-service" -> "sign-in service";
            case "chit-finance-service" -> "schemes and payments service";
            default -> serviceId;
        };
    }
}
