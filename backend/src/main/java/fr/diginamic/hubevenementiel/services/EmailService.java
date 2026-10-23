package fr.diginamic.hubevenementiel.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.config.FrontendUrls;
import fr.diginamic.hubevenementiel.services.EmailTemplate.Tone;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final FrontendUrls frontendUrls;

    @Value("${app.mail-sender}")
    private String senderAddress;

    public EmailService(JavaMailSender mailSender, FrontendUrls frontendUrls) {
        this.mailSender = mailSender;
        this.frontendUrls = frontendUrls;
    }

    public void sendVerificationEmail(String toEmail, String tokenValue) {
        send(toEmail, "MyHubEvent - Vérifiez votre compte!",
                EmailTemplate.of(Tone.INFO, "Bienvenue", "Activez votre compte MyHubEvent")
                        .paragraph("Il reste une étape : confirmer votre adresse email pour pouvoir vous inscrire aux évènements de vos clubs.")
                        .button("Activer mon compte", frontendUrls.current() + "/verify?token=" + tokenValue, false)
                        .showUrl()
                        .note("Ce lien expire dans 24 h. Si vous n'avez pas créé de compte, ignorez cet email."));
    }

    public void sendPasswordResetEmail(String toEmail, String tokenValue) {
        send(toEmail, "MyHubEvent - Confirmation de changement de mot de passe!",
                EmailTemplate.of(Tone.INFO, "Sécurité", "Choisissez un nouveau mot de passe")
                        .paragraph("Une demande de changement de mot de passe a été faite pour votre compte. Cliquez sur le bouton pour en choisir un nouveau.")
                        .button("Changer mon mot de passe", frontendUrls.current() + "/reset-password?token=" + tokenValue, false)
                        .showUrl()
                        .note("Ce lien expire dans 1 h. Si vous n'êtes pas à l'origine de cette demande, ignorez cet email : votre mot de passe ne change pas."));
    }

    public void sendPasswordChangeConfirmationEmail(String toEmail, String tokenValue) {
        send(toEmail, "MyHubEvent - Confirmez votre nouveau mot de passe",
                EmailTemplate.of(Tone.INFO, "Sécurité", "Confirmez votre nouveau mot de passe")
                        .paragraph("Vous avez choisi un nouveau mot de passe. Cliquez sur le bouton pour le confirmer et l'appliquer définitivement.")
                        .button("Confirmer le changement", frontendUrls.current() + "/confirm-password-change?token=" + tokenValue, false)
                        .showUrl()
                        .note("Ce lien expire dans 1 h. Si vous n'êtes pas à l'origine de cette demande, ignorez cet email."));
    }

    public void sendInscriptionCancellationEmail(String toEmail, MailEvent event, String motif) {
        send(toEmail, "MyHubEvent - Votre inscription a été annulée",
                EmailTemplate.of(Tone.WARN, "Inscription annulée", "L'organisateur a annulé votre inscription")
                        .paragraph("Votre inscription à l'évènement ci-dessous a été annulée par l'organisateur.")
                        .event(event, true)
                        .quote(motif)
                        .button("Voir l'évènement", eventUrl(event), false));
    }

    public void sendEventDeletedEmail(String toEmail, MailEvent event) {
        send(toEmail, "MyHubEvent - Un évènement a été supprimé",
                EmailTemplate.of(Tone.WARN, "Évènement supprimé", "Un évènement auquel vous étiez inscrit est supprimé")
                        .paragraph("L'organisateur a supprimé cet évènement. Votre inscription, ou votre place en liste d'attente, n'est donc plus valable.")
                        .event(event, true)
                        .button("Voir le catalogue", frontendUrls.current() + "/catalogue", false));
    }

    public void sendEventCancelledEmail(String toEmail, MailEvent event) {
        send(toEmail, "MyHubEvent - Un évènement a été annulé",
                EmailTemplate.of(Tone.WARN, "Annulé", "L'évènement auquel vous étiez inscrit est annulé")
                        .paragraph("L'organisateur a annulé cet évènement. Votre inscription, ou votre place en liste d'attente, n'est donc plus valable.")
                        .event(event, true)
                        .button("Retrouver mes évènements", frontendUrls.current() + "/my-past-events", true)
                        .note("L'évènement reste consultable dans « Mes évènements passés »."));
    }

    public void sendInscriptionPromotedEmail(String toEmail, MailEvent event) {
        send(toEmail, "MyHubEvent - Une place s'est libérée !",
                EmailTemplate.of(Tone.OK, "Place confirmée", "Bonne nouvelle, une place s'est libérée")
                        .paragraph("Vous étiez en liste d'attente : votre inscription est maintenant confirmée. Vous n'avez rien à faire.")
                        .event(event, false)
                        .button("Voir l'évènement", eventUrl(event), false)
                        .note("Un empêchement ? Vous pouvez vous désinscrire depuis la page de l'évènement pour libérer la place."));
    }

    public void sendAccountInfoUpdatedEmail(String toEmail) {
        send(toEmail, "MyHubEvent - Vos informations ont été modifiées",
                EmailTemplate.of(Tone.INFO, "Sécurité", "Vos informations ont été modifiées")
                        .paragraph("Les informations générales de votre compte (nom, prénom, email, téléphone ou adresse) viennent d'être modifiées.")
                        .button("Voir mon compte", frontendUrls.current() + "/account", false)
                        .note("Si vous n'êtes pas à l'origine de cette modification, contactez-nous au plus vite."));
    }

    public void sendAccountActivationEmail(String toEmail, String tokenValue, String temporaryPassword) {
        send(toEmail, "MyHubEvent - Activation de votre compte",
                EmailTemplate.of(Tone.INFO, "Compte créé", "Un compte a été créé pour vous")
                        .paragraph("Un compte a été créé pour vous sur MyHubEvent. Voici votre mot de passe temporaire :")
                        .highlight(temporaryPassword)
                        .paragraph("Cliquez sur le bouton pour activer le compte et choisir votre mot de passe définitif.")
                        .button("Activer mon compte", frontendUrls.current() + "/activate-account?token=" + tokenValue, false)
                        .showUrl()
                        .note("Ce lien expire dans 1 h."));
    }

    private String eventUrl(MailEvent event) {
        return frontendUrls.current() + "/event-details/" + event.id();
    }

    /** Un seul message : texte brut et HTML (clients qui n'affichent pas le HTML), logo joint en image intégrée. */
    private void send(String toEmail, String subject, EmailTemplate template) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(template.text(), template.html());
            helper.addInline(EmailTemplate.LOGO_CONTENT_ID, new ClassPathResource("mail/logo.png"), "image/png");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailPreparationException("Impossible de préparer l'email.", e);
        }
    }
}
