package fr.diginamic.hubevenementiel.services;

import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail-sender}")
    private String senderAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String tokenValue) {
        sendLinkEmail(toEmail, "MyHubEvent - Vérifiez votre compte!",
                "Bienvenue !\n\nCliquez sur ce lien pour activer votre compte :",
                frontendUrl + "/verify?token=" + tokenValue,
                "Ce lien expire dans 24h.");
    }

    public void sendPasswordResetEmail(String toEmail, String tokenValue) {
        sendLinkEmail(toEmail, "MyHubEvent - Confirmation de changement de mot de passe!",
                "Une demande de changement de mot de passe a été effectuée.\n\nCliquez sur ce lien pour confirmer :",
                frontendUrl + "/reset-password?token=" + tokenValue,
                "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\nCe lien expire dans 1h.");
    }

    public void sendPasswordChangeConfirmationEmail(String toEmail, String tokenValue) {
        sendLinkEmail(toEmail, "MyHubEvent - Confirmez votre nouveau mot de passe",
                "Vous avez choisi un nouveau mot de passe.\n\nCliquez sur ce lien pour confirmer et l'appliquer définitivement :",
                frontendUrl + "/confirm-password-change?token=" + tokenValue,
                "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\nCe lien expire dans 1h.");
    }

    public void sendInscriptionCancellationEmail(String toEmail, String eventTitle, String motif) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Votre inscription a été annulée");
        message.setText("Votre inscription à l'évènement \"" + eventTitle + "\" a été annulée par l'organisateur.\n\n"
                + "Motif : " + motif);

        mailSender.send(message);
    }

    public void sendEventDeletedEmail(String toEmail, String eventTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Un évènement a été supprimé");
        message.setText("L'évènement \"" + eventTitle + "\" a été supprimé par son organisateur.\n\n"
                + "Votre inscription, ou votre place en liste d'attente, n'est donc plus valable.");

        mailSender.send(message);
    }

    public void sendEventCancelledEmail(String toEmail, String eventTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Un évènement a été annulé");
        message.setText("L'évènement \"" + eventTitle + "\" a été annulé par son organisateur.\n\n"
                + "Votre inscription, ou votre place en liste d'attente, n'est donc plus valable.");

        mailSender.send(message);
    }

    public void sendInscriptionPromotedEmail(String toEmail, String eventTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Une place s'est libérée !");
        message.setText("Bonne nouvelle : une place s'est libérée pour l'évènement \"" + eventTitle + "\".\n\n"
                + "Votre inscription, jusqu'ici en liste d'attente, est maintenant confirmée.");

        mailSender.send(message);
    }

    public void sendAccountInfoUpdatedEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Vos informations ont été modifiées");
        message.setText("Les informations générales de votre compte (nom, prénom, email, téléphone ou adresse) "
                + "viennent d'être modifiées.\n\n"
                + "Si vous n'êtes pas à l'origine de cette modification, contactez-nous au plus vite.");

        mailSender.send(message);
    }

    public void sendAccountActivationEmail(String toEmail, String tokenValue, String temporaryPassword) {
        sendLinkEmail(toEmail, "MyHubEvent - Activation de votre compte",
                "Un compte a été créé pour vous sur MyHubEvent.\n\nMot de passe temporaire : " + temporaryPassword
                        + "\n\nCliquez sur ce lien pour l'activer et choisir votre mot de passe définitif :",
                frontendUrl + "/activate-account?token=" + tokenValue,
                "Ce lien expire dans 1h.");
    }

    /**
     * Sends a mail whose link is clickable: an HTML body, with the same text as plain-text fallback.
     */
    private void sendLinkEmail(String toEmail, String subject, String intro, String link, String outro) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            String plain = intro + "\n" + link + "\n\n" + outro;
            String html = "<p>" + toHtml(intro) + "</p>"
                    + "<p><a href=\"" + HtmlUtils.htmlEscape(link) + "\">" + HtmlUtils.htmlEscape(link) + "</a></p>"
                    + "<p>" + toHtml(outro) + "</p>";
            helper.setText(plain, html);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailPreparationException("Impossible de préparer l'email.", e);
        }
    }

    private static String toHtml(String text) {
        return HtmlUtils.htmlEscape(text).replace("\n\n", "</p><p>").replace("\n", "<br>");
    }
}
