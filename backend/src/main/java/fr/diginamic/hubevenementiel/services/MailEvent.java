package fr.diginamic.hubevenementiel.services;

import java.time.LocalDateTime;
import java.util.UUID;

import fr.diginamic.hubevenementiel.entities.Event;

/** Ce qu'un email a besoin de savoir d'un évènement : de quoi le décrire et y renvoyer. */
public record MailEvent(UUID id, String title, LocalDateTime start, String place, String club) {

    public static MailEvent of(Event event) {
        String place = event.getLocation() == null ? null : event.getLocation().getCity();
        String club = event.getClub() == null ? null : event.getClub().getName();
        return new MailEvent(event.getId(), event.getTitle(), event.getStartDateTime(), place, club);
    }
}
