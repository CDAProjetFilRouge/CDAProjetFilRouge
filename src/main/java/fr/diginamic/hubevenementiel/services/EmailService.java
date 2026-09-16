package fr.diginamic.hubevenementiel.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String tokenValue) {
        String verificationLink = frontendUrl + "/verify?token=" + tokenValue;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Vérifiez votre compte!");
        message.setText("Bienvenue !\n\nCliquez sur ce lien pour activer votre compte :\n"
                + verificationLink
                + "\n\nCe lien expire dans 24h.");

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String tokenValue) {
        String resetLink = frontendUrl + "/reset-password?token=" + tokenValue;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Confirmation de changement de mot de passe!");
        message.setText("Une demande de changement de mot de passe a été effectuée.\n\n"
                + "Cliquez sur ce lien pour confirmer :\n"
                + resetLink
                + "\n\nSi vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n"
                + "Ce lien expire dans 1h.");

        mailSender.send(message);
    }

    public void sendPasswordChangeConfirmationEmail(String toEmail, String tokenValue) {
        String confirmLink = frontendUrl + "/confirm-password-change?token=" + tokenValue;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Confirmez votre nouveau mot de passe");
        message.setText("Vous avez choisi un nouveau mot de passe.\n\n"
                + "Cliquez sur ce lien pour confirmer et l'appliquer définitivement :\n"
                + confirmLink
                + "\n\nSi vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n"
                + "Ce lien expire dans 1h.");

        mailSender.send(message);
    }

    public void sendInscriptionCancellationEmail(String toEmail, String eventTitle, String motif) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Votre inscription a été annulée");
        message.setText("Votre inscription à l'évènement \"" + eventTitle + "\" a été annulée par l'organisateur.\n\n"
                + "Motif : " + motif);

        mailSender.send(message);
    }
}
