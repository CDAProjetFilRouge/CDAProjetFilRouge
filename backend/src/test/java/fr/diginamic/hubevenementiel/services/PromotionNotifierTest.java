package fr.diginamic.hubevenementiel.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PromotionNotifierTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PromotionNotifier promotionNotifier;

    private static final MailEvent MARATHON = new MailEvent(uuid(2), "Marathon", java.time.LocalDateTime.of(2030, 5, 4, 9, 0), "Rennes", "Club A");

    @Test
    void onInscriptionPromoted_sendsTheEmail() {
        promotionNotifier.onInscriptionPromoted(new InscriptionPromotedEvent("alice@example.com", MARATHON));

        verify(emailService).sendInscriptionPromotedEmail("alice@example.com", MARATHON);
    }

    @Test
    void onInscriptionPromoted_mailFailure_isSwallowed() {
        doThrow(new MailSendException("SMTP indisponible"))
                .when(emailService).sendInscriptionPromotedEmail("alice@example.com", MARATHON);

        assertDoesNotThrow(() -> promotionNotifier
                .onInscriptionPromoted(new InscriptionPromotedEvent("alice@example.com", MARATHON)));
    }
}
