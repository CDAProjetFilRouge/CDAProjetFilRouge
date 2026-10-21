package fr.diginamic.hubevenementiel.services;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.repositories.EventRepo;

@Component
public class EventStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventStatusScheduler.class);

    private final EventRepo eventRepo;

    public EventStatusScheduler(EventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    /**
     * Every five minutes, and once at startup, marks as finished the published events whose end date has passed.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void finishEndedEvents() {
        int count = eventRepo.updateStatusOfEndedEvents(EventStatus.PUBLISHED, EventStatus.FINISHED,
                LocalDateTime.now());

        if (count > 0) {
            log.info("{} évènement(s) passé(s) automatiquement au statut terminé.", count);
        }
    }
}
