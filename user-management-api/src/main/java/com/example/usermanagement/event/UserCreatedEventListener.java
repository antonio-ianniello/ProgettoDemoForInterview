package com.example.usermanagement.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserCreatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCreatedEventListener.class);

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        LOGGER.info("Handled UserCreatedEvent for userId={}, email={}, roles={}", event.userId(), event.email(), event.roles());
    }
}
