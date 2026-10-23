package fr.diginamic.hubevenementiel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventCancellationNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventCancellationNotifier.class);

    private final EmailService emailService;

    public EventCancellationNotifier(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEventCancelled(EventCancelledEvent event) {
        for (String email : event.emails()) {
            try {
                emailService.sendEventCancelledEmail(email, event.event());
            } catch (RuntimeException e) {
                LOGGER.warn("Notification d'annulation non envoyée à {} : {}", email, e.getMessage());
            }
        }
    }
}
