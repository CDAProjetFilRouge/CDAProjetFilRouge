package fr.diginamic.hubevenementiel.services;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
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

    private static final MailEvent MARATHON = new MailEvent(uuid(2), "Marathon", java.time.LocalDateTime.of(2030, 5, 4, 9, 0), "Rennes", "Club A");

    @Test
    void onEventCancelled_sendsAnEmailToEveryone() {
        notifier.onEventCancelled(new EventCancelledEvent(List.of("alice@example.com", "bob@example.com"), MARATHON));

        verify(emailService).sendEventCancelledEmail("alice@example.com", MARATHON);
        verify(emailService).sendEventCancelledEmail("bob@example.com", MARATHON);
    }

    @Test
    void onEventCancelled_aMailFailureDoesNotStopTheOthers() {
        doThrow(new MailSendException("SMTP indisponible"))
                .when(emailService).sendEventCancelledEmail("alice@example.com", MARATHON);

        assertDoesNotThrow(() -> notifier
                .onEventCancelled(new EventCancelledEvent(List.of("alice@example.com", "bob@example.com"), MARATHON)));

        verify(emailService).sendEventCancelledEmail("bob@example.com", MARATHON);
    }
}
