package fr.diginamic.hubevenementiel.services;

import java.util.List;

public record EventCancelledEvent(List<String> emails, String eventTitle) {
}
