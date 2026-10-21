package fr.diginamic.hubevenementiel.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class EventCancellationNotifierTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EventCancellationNotifier notifier;

    @Test
    void onEventCancelled_sendsAnEmailToEveryone() {
        notifier.onEventCancelled(new EventCancelledEvent(List.of("alice@example.com", "bob@example.com"), "Marathon"));

        verify(emailService).sendEventCancelledEmail("alice@example.com", "Marathon");
        verify(emailService).sendEventCancelledEmail("bob@example.com", "Marathon");
    }

    @Test
    void onEventCancelled_aMailFailureDoesNotStopTheOthers() {
        doThrow(new MailSendException("SMTP indisponible"))
                .when(emailService).sendEventCancelledEmail("alice@example.com", "Marathon");

        assertDoesNotThrow(() -> notifier
                .onEventCancelled(new EventCancelledEvent(List.of("alice@example.com", "bob@example.com"), "Marathon")));

        verify(emailService).sendEventCancelledEmail("bob@example.com", "Marathon");
    }
}
