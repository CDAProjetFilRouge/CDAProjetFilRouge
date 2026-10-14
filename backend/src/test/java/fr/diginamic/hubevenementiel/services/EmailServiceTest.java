package fr.diginamic.hubevenementiel.services;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://hub.example.org");
        ReflectionTestUtils.setField(emailService, "senderAddress", "no-reply@example.org");
    }

    @Test
    void verificationEmail_hasAClickableLinkBuiltFromTheConfiguredFrontendUrl() throws Exception {
        emailService.sendVerificationEmail("membre@example.org", "abc123");

        String html = htmlBodyOfSentMail();
        assertThat(html).contains("<a href=\"https://hub.example.org/verify?token=abc123\">");
    }

    @Test
    void activationEmail_hasAClickableLinkAndEscapesTheTemporaryPassword() throws Exception {
        emailService.sendAccountActivationEmail("membre@example.org", "tok", "a<b&c");

        String html = htmlBodyOfSentMail();
        assertThat(html).contains("<a href=\"https://hub.example.org/activate-account?token=tok\">");
        assertThat(html).contains("a&lt;b&amp;c");
    }

    @Test
    void passwordEmails_haveClickableLinks() throws Exception {
        emailService.sendPasswordResetEmail("membre@example.org", "r1");
        emailService.sendPasswordChangeConfirmationEmail("membre@example.org", "c1");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(captor.capture());
        assertThat(findPart(captor.getAllValues().get(0), "text/html"))
                .contains("<a href=\"https://hub.example.org/reset-password?token=r1\">");
        assertThat(findPart(captor.getAllValues().get(1), "text/html"))
                .contains("<a href=\"https://hub.example.org/confirm-password-change?token=c1\">");
    }

    private String htmlBodyOfSentMail() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return findPart(captor.getValue(), "text/html");
    }

    private static String findPart(Object content, String mimeType) throws Exception {
        if (content instanceof MimeMessage message) {
            message.saveChanges();
            if (message.isMimeType(mimeType)) {
                return (String) message.getContent();
            }
            return findPart(message.getContent(), mimeType);
        }
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType(mimeType)) {
                    return (String) part.getContent();
                }
                String found = findPart(part.getContent(), mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
