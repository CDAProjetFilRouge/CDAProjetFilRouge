package fr.diginamic.hubevenementiel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventDeletionNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDeletionNotifier.class);

    private final EmailService emailService;

    public EventDeletionNotifier(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Tells every registered member and every waiting-list member that the event is gone. Sent once the
     * deletion is committed, and a mail failure for one person neither cancels the deletion nor stops the others.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEventDeleted(EventDeletedEvent event) {
        for (String email : event.emails()) {
            try {
                emailService.sendEventDeletedEmail(email, event.eventTitle());
            } catch (RuntimeException e) {
                LOGGER.warn("Notification de suppression non envoyée à {} : {}", email, e.getMessage());
            }
        }
    }
}
