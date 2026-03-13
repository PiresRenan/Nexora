package com.nexora.adapter.output.event;

import com.nexora.domain.event.DomainEvent;
import com.nexora.domain.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisherAdapter implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisherAdapter.class);

    @Override
    public void publish(DomainEvent event) {
        log.info("Domain event published: {}", event.getClass().getSimpleName());
    }
}
