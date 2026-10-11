package com.finsphere.auth.outbox;

import java.time.Clock;

import tools.jackson.databind.json.JsonMapper;

import com.finsphere.common.web.CorrelationId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes outgoing Kafka messages into the outbox table inside the caller's transaction: the message exists if
 * and only if the business change committed.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository repository;

    private final JsonMapper jsonMapper;

    private final Clock clock;

    public OutboxService(OutboxEventRepository repository, JsonMapper jsonMapper, Clock clock) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String key, Object payload) {
        String json = this.jsonMapper.writeValueAsString(payload);
        this.repository.save(new OutboxEvent(topic, key, json, CorrelationId.current(), this.clock.instant()));
    }
}
