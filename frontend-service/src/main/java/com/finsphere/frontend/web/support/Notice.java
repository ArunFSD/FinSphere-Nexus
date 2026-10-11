package com.finsphere.frontend.web.support;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

/**
 * Message shown after a redirect. Only these fixed keys are accepted in the {@code notice} query parameter, so
 * nothing from the URL is ever echoed into the page.
 */
public enum Notice {

    SIGNED_OUT("signed-out", Tone.INFO, "You have signed out."),
    SESSION_ENDED("session-ended", Tone.INFO, "Your session ended. Sign in again to continue."),
    REGISTERED("registered", Tone.SUCCESS, "Account created. Sign in with your mobile number and password."),
    FORM_EXPIRED("form-expired", Tone.WARNING,
            "This page had expired, so nothing was saved. Fill in the form again and send it."),
    SCHEME_CREATED("scheme-created", Tone.SUCCESS, "Scheme created with its monthly schedule."),
    SLOT_BOOKED("slot-booked", Tone.SUCCESS, "Slot booked."),
    AUCTION_SETTLED("auction-settled", Tone.SUCCESS, "Auction settled and the month is closed."),
    LOAN_DISBURSED("loan-disbursed", Tone.SUCCESS, "Loan disbursed."),
    PAYMENT_RECORDED("payment-recorded", Tone.SUCCESS, "Payment recorded."),
    MEMBER_ADDED("member-added", Tone.SUCCESS, "Member added. They appear in member lists within a few seconds.");

    private final String key;

    private final Tone tone;

    private final String message;

    Notice(String key, Tone tone, String message) {
        this.key = key;
        this.tone = tone;
        this.message = message;
    }

    public String key() {
        return this.key;
    }

    public Tone tone() {
        return this.tone;
    }

    public String message() {
        return this.message;
    }

    public static @Nullable Notice of(@Nullable String key) {
        if (key == null) {
            return null;
        }
        return Arrays.stream(values()).filter((notice) -> notice.key.equals(key)).findFirst().orElse(null);
    }

    public static @Nullable String messageFor(@Nullable String key) {
        Notice notice = of(key);
        return (notice != null) ? notice.message : null;
    }

    /** How the message looks: the alert style and icon the templates use. */
    public enum Tone {

        SUCCESS("alert-success", "#i-check"),
        INFO("alert-info", "#i-info"),
        WARNING("alert-error", "#i-alert");

        private final String alertClass;

        private final String icon;

        Tone(String alertClass, String icon) {
            this.alertClass = alertClass;
            this.icon = icon;
        }

        public String alertClass() {
            return this.alertClass;
        }

        public String icon() {
            return this.icon;
        }
    }
}
