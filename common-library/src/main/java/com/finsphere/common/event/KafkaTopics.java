package com.finsphere.common.event;

/**
 * Kafka topic names shared between producers and consumers.
 */
public final class KafkaTopics {

    /** User directory changes, keyed by user id. */
    public static final String USER_UPDATES = "user-updates-topic";

    /** Records that could not be processed after retries end up here for inspection and replay. */
    public static final String USER_UPDATES_DLT = USER_UPDATES + "-dlt";

    private KafkaTopics() {
    }
}
