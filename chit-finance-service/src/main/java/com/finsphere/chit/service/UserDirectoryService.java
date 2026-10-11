package com.finsphere.chit.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.chit.domain.UserMirror;
import com.finsphere.chit.mapper.UserMirrorMapper;
import com.finsphere.chit.repository.jpa.UserMirrorRepository;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.common.event.UserUpdateEvent;
import com.finsphere.common.exception.DomainException;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This service's read-only copy of the user directory, fed by auth-service events.
 */
@Service
public class UserDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(UserDirectoryService.class);

    private final UserMirrorRepository repository;

    private final UserMirrorMapper mapper;

    private final Clock clock;

    public UserDirectoryService(UserMirrorRepository repository, UserMirrorMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** The member, or a 404 the UI can show next to the member field. */
    @Transactional(readOnly = true)
    public UserMirror requireActiveMember(Long userId) {
        return this.repository.findByUserIdAndActiveTrue(userId)
            .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Member not found", "userId",
                    "No active member with id " + userId + ". New registrations appear here within a few seconds."));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> namesOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return this.repository.findAllById(userIds)
            .stream()
            .filter((mirror) -> mirror.getFullName() != null)
            .collect(Collectors.toMap(UserMirror::getUserId, UserMirror::getFullName, (first, second) -> first));
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> activeMembers(Pageable pageable) {
        return PageResponse.from(this.repository.findByActiveTrue(pageable), this.mapper::toMember);
    }

    /**
     * Idempotent, order-aware upsert: replaying an event leaves the row unchanged and an event older than the
     * stored state (a redelivery after a newer update) is ignored.
     */
    @Transactional
    public void apply(UserUpdateEvent event) {
        Instant occurredAt = (event.occurredAt() != null) ? event.occurredAt() : this.clock.instant();
        Optional<UserMirror> current = this.repository.findById(event.userId());
        if (current.isPresent() && current.get().getLastUpdatedAt() != null
                && current.get().getLastUpdatedAt().isAfter(occurredAt)) {
            log.info("Ignoring stale update for user {} ({} is older than {})", event.userId(), occurredAt,
                    current.get().getLastUpdatedAt());
            return;
        }
        UserMirror mirror = current.map((existing) -> {
            this.mapper.update(event, existing);
            return existing;
        }).orElseGet(() -> this.mapper.toEntity(event));
        mirror.setLastUpdatedAt(occurredAt);
        this.repository.save(mirror);
        log.info("Mirrored user {} (role {}, active {})", event.userId(), event.role(), event.active());
    }
}
