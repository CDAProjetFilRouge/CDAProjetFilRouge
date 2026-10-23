package fr.diginamic.hubevenementiel.services;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.util.HtmlUtils;

/**
 * Gabarit commun des emails : en-tête avec le logo, pastille de contexte, titre, paragraphes, billet de l'évènement,
 * bouton, note. Le même contenu produit la version HTML (tableaux et styles en ligne, pour les messageries) et la
 * version texte brut.
 */
public final class EmailTemplate {

    public enum Tone {
        INFO("#dfe5ff", "#1f37a8"), OK("#d6ebe2", "#14513c"), WARN("#fde3da", "#b3361a");

        private final String background;
        private final String text;

        Tone(String background, String text) {
            this.background = background;
            this.text = text;
        }
    }

    public static final String LOGO_CONTENT_ID = "logo";

    private static final String FONT = "'Inter','Segoe UI',Helvetica,Arial,sans-serif";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d", Locale.FRENCH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);

    private final Tone tone;
    private final String tag;
    private final String title;
    private final List<String> paragraphs = new ArrayList<>();
    private MailEvent event;
    private boolean eventOff;
    private String quote;
    private String highlight;
    private int highlightAfter;
    private String buttonLabel;
    private String buttonUrl;
    private boolean pine;
    private boolean showUrl;
    private String note;

    private EmailTemplate(Tone tone, String tag, String title) {
        this.tone = tone;
        this.tag = tag;
        this.title = title;
    }

    public static EmailTemplate of(Tone tone, String tag, String title) {
        return new EmailTemplate(tone, tag, title);
    }

    public EmailTemplate paragraph(String text) {
        paragraphs.add(text);
        return this;
    }

    /** Le billet de l'évènement ; {@code off} le grise et le barre (annulé, supprimé). */
    public EmailTemplate event(MailEvent event, boolean off) {
        this.event = event;
        this.eventOff = off;
        return this;
    }

    /** Citation mise en retrait (le motif d'une annulation). */
    public EmailTemplate quote(String text) {
        this.quote = text;
        return this;
    }

    /** Valeur à recopier, mise en évidence (un mot de passe temporaire). */
    public EmailTemplate highlight(String text) {
        this.highlight = text;
        this.highlightAfter = paragraphs.size();
        return this;
    }

    public EmailTemplate button(String label, String url, boolean pine) {
        this.buttonLabel = label;
        this.buttonUrl = url;
        this.pine = pine;
        return this;
    }

    /** Écrit aussi l'adresse du bouton en toutes lettres, pour les messageries qui bloquent les boutons. */
    public EmailTemplate showUrl() {
        this.showUrl = true;
        return this;
    }

    public EmailTemplate note(String text) {
        this.note = text;
        return this;
    }

    // ------------------------------------------------------------------ texte brut

    public String text() {
        StringBuilder out = new StringBuilder();
        out.append(title).append("\n\n");
        for (int i = 0; i <= paragraphs.size(); i++) {
            if (highlight != null && i == highlightAfter) {
                out.append(highlight).append("\n\n");
            }
            if (i < paragraphs.size()) {
                out.append(paragraphs.get(i)).append("\n\n");
            }
        }
        if (event != null) {
            out.append("Évènement : ").append(event.title()).append('\n');
            if (event.start() != null) {
                out.append("Quand : ").append(capitalize(event.start().format(WHEN))).append('\n');
            }
            if (event.place() != null) {
                out.append("Où : ").append(event.place()).append('\n');
            }
            if (event.club() != null) {
                out.append("Club : ").append(event.club()).append('\n');
            }
            out.append('\n');
        }
        if (quote != null) {
            out.append("Motif : ").append(quote).append("\n\n");
        }
        if (buttonUrl != null) {
            out.append(buttonLabel).append(" : ").append(buttonUrl).append("\n\n");
        }
        if (note != null) {
            out.append(note).append("\n\n");
        }
        out.append("MyHubEvent - Message automatique, merci de ne pas y répondre.");
        return out.toString();
    }

    // ------------------------------------------------------------------ HTML

    public String html() {
        StringBuilder h = new StringBuilder(4096);
        h.append("<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<meta name=\"color-scheme\" content=\"light\"><title>MyHubEvent</title></head>")
                .append("<body style=\"margin:0;padding:0;background:#f4efe3;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4efe3;\">")
                .append("<tr><td align=\"center\" style=\"padding:24px 12px;\">")
                .append("<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:560px;")
                .append("background:#ffffff;border:1px solid #e3dcc8;border-radius:12px;font-family:").append(FONT)
                .append(";color:#17182b;\">");

        h.append("<tr><td style=\"padding:22px 28px 16px;\"><table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr>")
                .append("<td style=\"padding-right:10px;\"><img src=\"cid:").append(LOGO_CONTENT_ID)
                .append("\" width=\"46\" height=\"38\" alt=\"\" style=\"display:block;border:0;\"></td>")
                .append("<td style=\"font-size:22px;font-weight:700;letter-spacing:-0.04em;color:#17182b;\">MyHub<span style=\"color:#2a46c9;\">Event</span></td>")
                .append("</tr></table></td></tr>");
        h.append("<tr><td style=\"padding:0 28px;\"><div style=\"border-top:2px dashed #e3dcc8;height:0;line-height:0;font-size:0;\">&nbsp;</div></td></tr>");

        h.append("<tr><td style=\"padding:22px 28px 6px;\">");
        h.append("<span style=\"display:inline-block;font-size:11px;font-weight:700;letter-spacing:0.06em;text-transform:uppercase;")
                .append("padding:3px 10px;border-radius:999px;background:").append(tone.background).append(";color:")
                .append(tone.text).append(";\">").append(esc(tag)).append("</span>");
        h.append("<h1 style=\"margin:12px 0 12px;font-size:22px;line-height:1.25;letter-spacing:-0.02em;color:#17182b;\">")
                .append(esc(title)).append("</h1>");
        for (int i = 0; i <= paragraphs.size(); i++) {
            if (highlight != null && i == highlightAfter) {
                h.append("<p style=\"margin:0 0 16px;padding:12px 16px;background:#f4efe3;border-radius:8px;font-family:Consolas,Menlo,monospace;")
                        .append("font-size:16px;letter-spacing:0.04em;color:#17182b;\">").append(esc(highlight)).append("</p>");
            }
            if (i < paragraphs.size()) {
                h.append("<p style=\"margin:0 0 14px;font-size:15px;line-height:1.6;color:#17182b;\">").append(esc(paragraphs.get(i))).append("</p>");
            }
        }
        if (event != null) {
            appendStub(h);
        }
        if (quote != null) {
            h.append("<p style=\"margin:0 0 18px;padding:10px 16px;border-left:3px solid #e2622b;background:#faf7ef;font-size:14.5px;line-height:1.55;color:#17182b;\">")
                    .append("<strong>Motif :</strong> ").append(esc(quote)).append("</p>");
        }
        if (buttonUrl != null) {
            h.append("<p style=\"margin:6px 0 0;\"><a href=\"").append(esc(buttonUrl)).append("\" style=\"display:inline-block;background:")
                    .append(pine ? "#1d5a45" : "#2a46c9").append(";color:#ffffff;text-decoration:none;font-weight:600;font-size:15px;")
                    .append("padding:13px 26px;border-radius:10px;\">").append(esc(buttonLabel)).append("</a></p>");
            if (showUrl) {
                h.append("<p style=\"margin:16px 0 0;font-size:12.5px;line-height:1.5;color:#5b5d73;word-break:break-all;\">")
                        .append("Le bouton ne s'affiche pas ? Copiez cette adresse dans votre navigateur :<br><a href=\"")
                        .append(esc(buttonUrl)).append("\" style=\"color:#2a46c9;\">").append(esc(buttonUrl)).append("</a></p>");
            }
        }
        if (note != null) {
            h.append("<p style=\"margin:18px 0 0;font-size:13px;line-height:1.55;color:#5b5d73;\">").append(esc(note).replace("\n", "<br>")).append("</p>");
        }
        h.append("</td></tr>");

        h.append("<tr><td style=\"padding:20px 28px 26px;\"><div style=\"border-top:1px solid #e3dcc8;padding-top:16px;font-size:12.5px;line-height:1.55;color:#5b5d73;\">")
                .append("<strong style=\"color:#17182b;\">MyHubEvent</strong> · plateforme d'évènements de la fédération régionale de clubs.<br>")
                .append("Vous recevez cet email parce qu'un compte existe avec cette adresse. Message automatique, merci de ne pas y répondre.")
                .append("</div></td></tr>");
        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    private void appendStub(StringBuilder h) {
        String dateBackground = eventOff ? "#8c8fa3" : "#2a46c9";
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:18px 0 22px;border:1px solid #e3dcc8;border-radius:12px;border-collapse:separate;overflow:hidden;\"><tr>");
        if (event.start() != null) {
            h.append("<td width=\"84\" align=\"center\" style=\"width:84px;background:").append(dateBackground)
                    .append(";color:#ffffff;padding:14px 8px;\"><div style=\"font-size:30px;font-weight:700;line-height:1;letter-spacing:-0.03em;\">")
                    .append(event.start().format(DAY)).append("</div><div style=\"font-size:12px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;\">")
                    .append(esc(event.start().format(MONTH))).append("</div></td>");
        }
        h.append("<td style=\"padding:14px 18px;border-left:2px dashed #e3dcc8;\">")
                .append("<div style=\"font-size:16px;font-weight:700;line-height:1.3;color:").append(eventOff ? "#5b5d73" : "#17182b")
                .append(eventOff ? ";text-decoration:line-through;" : ";").append("\">").append(esc(event.title())).append("</div>");
        if (event.start() != null) {
            h.append("<div style=\"font-size:13.5px;color:#5b5d73;margin-top:3px;\">")
                    .append(esc(capitalize(event.start().format(WHEN)))).append("</div>");
        }
        String where = event.place() != null && event.club() != null ? event.place() + " · " + event.club()
                : event.place() != null ? event.place() : event.club();
        if (where != null) {
            h.append("<div style=\"font-size:13.5px;color:#5b5d73;margin-top:2px;\">").append(esc(where)).append("</div>");
        }
        h.append("</td></tr></table>");
    }

    private static String esc(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8");
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
