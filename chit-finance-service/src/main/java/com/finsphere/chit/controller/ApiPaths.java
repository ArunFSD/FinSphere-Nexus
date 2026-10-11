package com.finsphere.chit.controller;

/**
 * Public URL layout of this service (routed by the gateway on {@code /chits/**} and {@code /finance/**}).
 */
public final class ApiPaths {

    public static final String CHITS = "/chits";

    public static final String PLANS = CHITS + "/plans";

    public static final String ENROLLMENTS = CHITS + "/enrollments";

    public static final String CYCLES = CHITS + "/cycles";

    public static final String MEMBERS = CHITS + "/members";

    public static final String FINANCE = "/finance";

    public static final String LOANS = FINANCE + "/loans";

    public static final String TRANSACTIONS = FINANCE + "/transactions";

    private ApiPaths() {
    }
}
