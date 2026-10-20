package fr.diginamic.hubevenementiel.repositories;

import java.util.UUID;

public interface EventInscriptionCount {

    UUID getEventId();

    Long getTotal();
}
