package fr.diginamic.hubevenementiel.services;

import java.util.List;

public record EventDeletedEvent(List<String> emails, String eventTitle) {
}
