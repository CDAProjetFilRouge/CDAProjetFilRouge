package fr.diginamic.hubevenementiel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PromotionNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromotionNotifier.class);

    private final EmailService emailService;

    public PromotionNotifier(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Tells a member that a spot was given to them. Sent once the promotion is
     * committed, and a mail failure never cancels the promotion.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInscriptionPromoted(InscriptionPromotedEvent event) {
        try {
            emailService.sendInscriptionPromotedEmail(event.email(), event.eventTitle());
        } catch (RuntimeException e) {
            LOGGER.warn("Notification de promotion non envoyée à {} : {}", event.email(), e.getMessage());
        }
    }
}
