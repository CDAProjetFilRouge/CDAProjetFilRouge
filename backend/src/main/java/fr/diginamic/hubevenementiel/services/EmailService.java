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

    @Value("${app.mail-sender}")
    private String senderAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String tokenValue) {
        String verificationLink = frontendUrl + "/verify?token=" + tokenValue;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
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
        message.setFrom(senderAddress);
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
        message.setFrom(senderAddress);
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
        String activationLink = frontendUrl + "/activate-account?token=" + tokenValue;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(toEmail);
        message.setSubject("MyHubEvent - Activation de votre compte");
        message.setText("Un compte a été créé pour vous sur MyHubEvent.\n\n"
                + "Mot de passe temporaire : " + temporaryPassword + "\n\n"
                + "Cliquez sur ce lien pour l'activer et choisir votre mot de passe définitif :\n"
                + activationLink
                + "\n\nCe lien expire dans 1h.");

        mailSender.send(message);
    }
}
