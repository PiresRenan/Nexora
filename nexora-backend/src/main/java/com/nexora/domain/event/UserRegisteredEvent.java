package com.nexora.domain.event;

import com.nexora.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID     eventId,
        Instant  occurredAt,
        UUID     userId,
        String   email,
        UserRole role
) implements DomainEvent {

    @Override public String eventType()    { return "user.registered"; }
    @Override public String aggregateType(){ return "User"; }
    @Override public UUID   aggregateId()  { return userId; }

    public static UserRegisteredEvent of(UUID userId, String email, UserRole role) {
        return new UserRegisteredEvent(UUID.randomUUID(), Instant.now(), userId, email, role);
    }
}