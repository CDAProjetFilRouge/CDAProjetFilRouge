package fr.diginamic.hubevenementiel.services;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import fr.diginamic.hubevenementiel.config.FrontendUrls;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

class EmailServiceTest {

    private static final MailEvent MARATHON = new MailEvent(uuid(2), "Marathon de <Rennes>",
            LocalDateTime.of(2030, 5, 4, 9, 0), "Rennes", "Club Athlétisme Rennais");

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        emailService = new EmailService(mailSender, new FrontendUrls("https://hub.example.org", ""));
        ReflectionTestUtils.setField(emailService, "senderAddress", "no-reply@example.org");
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ------------------------------------------------------------------ liens des emails à jeton

    @Test
    void verificationEmail_hasAClickableButtonAndTheFullAddressWritten() throws Exception {
        emailService.sendVerificationEmail("membre@example.org", "abc123");

        MimeMessage mail = sent();
        String html = part(mail, "text/html");
        assertThat(html).contains("href=\"https://hub.example.org/verify?token=abc123\"")
                .contains("Activer mon compte")
                .contains(">https://hub.example.org/verify?token=abc123</a>");
        assertThat(part(mail, "text/plain")).contains("Activer mon compte : https://hub.example.org/verify?token=abc123");
    }

    @Test
    void activationEmail_hasAClickableButtonAndEscapesTheTemporaryPassword() throws Exception {
        emailService.sendAccountActivationEmail("membre@example.org", "tok", "a<b&c");

        MimeMessage mail = sent();
        String html = part(mail, "text/html");
        assertThat(html).contains("href=\"https://hub.example.org/activate-account?token=tok\"");
        assertThat(html).contains("a&lt;b&amp;c").doesNotContain("a<b&c");
        assertThat(html.indexOf("mot de passe temporaire")).isLessThan(html.indexOf("a&lt;b&amp;c"));
        assertThat(html.indexOf("a&lt;b&amp;c")).isLessThan(html.indexOf("Cliquez sur le bouton"));
        assertThat(part(mail, "text/plain")).containsSubsequence("mot de passe temporaire", "a<b&c", "Cliquez sur le bouton");
        assertThat(part(mail, "text/plain")).contains("a<b&c");
    }

    @Test
    void passwordEmails_haveClickableButtons() throws Exception {
        emailService.sendPasswordResetEmail("membre@example.org", "r1");
        emailService.sendPasswordChangeConfirmationEmail("membre@example.org", "c1");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(captor.capture());
        assertThat(part(captor.getAllValues().get(0), "text/html"))
                .contains("href=\"https://hub.example.org/reset-password?token=r1\"");
        assertThat(part(captor.getAllValues().get(1), "text/html"))
                .contains("href=\"https://hub.example.org/confirm-password-change?token=c1\"");
    }

    @Test
    void links_followTheFrontThatSentTheRequest_onlyWhenItIsAllowed() throws Exception {
        emailService = new EmailService(mailSender,
                new FrontendUrls("https://hub.example.org", "https://autre.example.org"));
        ReflectionTestUtils.setField(emailService, "senderAddress", "no-reply@example.org");

        requestFrom("https://autre.example.org");
        emailService.sendPasswordResetEmail("membre@example.org", "r1");
        requestFrom("https://evil.example.com");
        emailService.sendPasswordResetEmail("membre@example.org", "r2");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, org.mockito.Mockito.times(2)).send(captor.capture());
        assertThat(part(captor.getAllValues().get(0), "text/html"))
                .contains("href=\"https://autre.example.org/reset-password?token=r1\"");
        assertThat(part(captor.getAllValues().get(1), "text/html"))
                .contains("href=\"https://hub.example.org/reset-password?token=r2\"")
                .doesNotContain("evil");
    }

    // ------------------------------------------------------------------ emails d'évènement

    @Test
    void promotedEmail_confirmsThePlaceAndLinksToTheEvent() throws Exception {
        emailService.sendInscriptionPromotedEmail("membre@example.org", MARATHON);

        MimeMessage mail = sent();
        String html = part(mail, "text/html");
        assertThat(mail.getSubject()).isEqualTo("MyHubEvent - Une place s'est libérée !");
        assertThat(html).contains("Place confirmée")
                .contains("href=\"https://hub.example.org/event-details/" + uuid(2) + "\"")
                .contains("Voir l&#39;évènement")
                .contains("Marathon de &lt;Rennes&gt;")
                .doesNotContain("Marathon de <Rennes>")
                .contains(">4<").contains("Samedi 4 mai 2030 à 09h00").contains("Rennes · Club Athlétisme Rennais")
                .doesNotContain("line-through");
        assertThat(part(mail, "text/plain")).contains("Évènement : Marathon de <Rennes>")
                .contains("Voir l'évènement : https://hub.example.org/event-details/" + uuid(2));
    }

    @Test
    void cancelledEmail_greysOutTheEventAndLinksToThePastEvents() throws Exception {
        emailService.sendEventCancelledEmail("membre@example.org", MARATHON);

        String html = part(sent(), "text/html");
        assertThat(html).contains("line-through").contains("Annulé")
                .contains("href=\"https://hub.example.org/my-past-events\"")
                .contains("Retrouver mes évènements");
    }

    @Test
    void deletedEmail_greysOutTheEventAndLinksToTheCatalogueNotToTheMissingEvent() throws Exception {
        emailService.sendEventDeletedEmail("membre@example.org", MARATHON);

        String html = part(sent(), "text/html");
        assertThat(html).contains("line-through").contains("href=\"https://hub.example.org/catalogue\"")
                .doesNotContain("event-details");
    }

    @Test
    void inscriptionCancellationEmail_quotesTheReasonEscapedAndHasAFromAddress() throws Exception {
        emailService.sendInscriptionCancellationEmail("membre@example.org", MARATHON, "Salle <fermée> & travaux");

        MimeMessage mail = sent();
        assertThat(mail.getFrom()[0].toString()).isEqualTo("no-reply@example.org");
        assertThat(part(mail, "text/html")).contains("Motif :").contains("Salle &lt;fermée&gt; &amp; travaux")
                .doesNotContain("Salle <fermée>");
        assertThat(part(mail, "text/plain")).contains("Motif : Salle <fermée> & travaux");
    }

    @Test
    void everyMail_carriesTheLogoAsAnInlineImage() throws Exception {
        emailService.sendEventCancelledEmail("membre@example.org", MARATHON);

        MimeMessage mail = sent();
        mail.saveChanges();
        assertThat(part(mail, "text/html")).contains("src=\"cid:logo\"");
        assertThat(inlineImageTypes(mail.getContent())).contains("image/png");
    }

    @Test
    void accountInfoUpdatedEmail_linksToTheAccount() throws Exception {
        emailService.sendAccountInfoUpdatedEmail("membre@example.org");

        assertThat(part(sent(), "text/html")).contains("href=\"https://hub.example.org/account\"")
                .contains("contactez-nous au plus vite");
    }

    // ------------------------------------------------------------------ utilitaires

    private static void requestFrom(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", origin);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private MimeMessage sent() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private static java.util.List<String> inlineImageTypes(Object content) throws Exception {
        java.util.List<String> types = new java.util.ArrayList<>();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("image/png")) {
                    types.add("image/png");
                }
                types.addAll(inlineImageTypes(bodyPart.getContent()));
            }
        }
        return types;
    }

    private static String part(Object content, String mimeType) throws Exception {
        if (content instanceof MimeMessage message) {
            message.saveChanges();
            if (message.isMimeType(mimeType)) {
                return (String) message.getContent();
            }
            return part(message.getContent(), mimeType);
        }
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType(mimeType)) {
                    return (String) bodyPart.getContent();
                }
                String found = part(bodyPart.getContent(), mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
