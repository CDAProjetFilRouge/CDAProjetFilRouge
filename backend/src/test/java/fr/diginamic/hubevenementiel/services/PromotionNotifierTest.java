package fr.diginamic.hubevenementiel.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PromotionNotifierTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PromotionNotifier promotionNotifier;

    @Test
    void onInscriptionPromoted_sendsTheEmail() {
        promotionNotifier.onInscriptionPromoted(new InscriptionPromotedEvent("alice@example.com", "Marathon"));

        verify(emailService).sendInscriptionPromotedEmail("alice@example.com", "Marathon");
    }

    @Test
    void onInscriptionPromoted_mailFailure_isSwallowed() {
        doThrow(new MailSendException("SMTP indisponible"))
                .when(emailService).sendInscriptionPromotedEmail("alice@example.com", "Marathon");

        assertDoesNotThrow(() -> promotionNotifier
                .onInscriptionPromoted(new InscriptionPromotedEvent("alice@example.com", "Marathon")));
    }
}
