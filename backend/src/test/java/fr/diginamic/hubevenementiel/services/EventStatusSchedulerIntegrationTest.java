package fr.diginamic.hubevenementiel.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class EventStatusSchedulerIntegrationTest {

    @Autowired
    private EventStatusScheduler scheduler;
    @Autowired
    private EventRepo eventRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ClubRepo clubRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    private AppUser organizer() {
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("Organisateur");
        user.setEmail("scheduler-" + System.nanoTime() + "@example.com");
        user.setPhone("0600000000");
        user.setHashedPassword(passwordEncoder.encode("motdepasse123456"));
        user.setRole(Role.ORGANIZER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setClubs(List.of());
        return userRepo.save(user);
    }

    private Event event(AppUser organizer, Club club, EventStatus status, LocalDateTime start, LocalDateTime end) {
        Event event = new Event();
        event.setTitle("Event-" + System.nanoTime());
        event.setDescription("desc");
        event.setCategory(Category.SPORT);
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setMaxCapacity(10);
        event.setAffiliatePrice(BigDecimal.TEN);
        event.setNonAffiliatePrice(BigDecimal.valueOf(20));
        event.setStatus(status);
        event.setOrganizer(organizer);
        event.setClub(club);
        return eventRepo.save(event);
    }

    private EventStatus reloadedStatusOf(Event event) {
        entityManager.flush();
        entityManager.clear();
        return eventRepo.findById(event.getId()).orElseThrow().getStatus();
    }

    @Test
    void finishEndedEvents_finishesOnlyPublishedEventsWhoseEndHasPassed() {
        AppUser organizer = organizer();
        Club club = new Club();
        club.setName("Club-" + System.nanoTime());
        club.setCategory(Category.SPORT);
        club.setEmail("club@example.com");
        club.setPhone("0600000000");
        club = clubRepo.save(club);
        LocalDateTime now = LocalDateTime.now();

        Event ended = event(organizer, club, EventStatus.PUBLISHED, now.minusHours(3), now.minusHours(1));
        Event ongoing = event(organizer, club, EventStatus.PUBLISHED, now.minusHours(1), now.plusHours(1));
        Event upcoming = event(organizer, club, EventStatus.PUBLISHED, now.plusDays(1), now.plusDays(1).plusHours(2));
        Event draft = event(organizer, club, EventStatus.DRAFT, now.minusHours(3), now.minusHours(1));
        Event cancelled = event(organizer, club, EventStatus.CANCELLED, now.minusHours(3), now.minusHours(1));

        scheduler.finishEndedEvents();

        assertThat(reloadedStatusOf(ended)).isEqualTo(EventStatus.FINISHED);
        assertThat(reloadedStatusOf(ongoing)).isEqualTo(EventStatus.PUBLISHED);
        assertThat(reloadedStatusOf(upcoming)).isEqualTo(EventStatus.PUBLISHED);
        assertThat(reloadedStatusOf(draft)).isEqualTo(EventStatus.DRAFT);
        assertThat(reloadedStatusOf(cancelled)).isEqualTo(EventStatus.CANCELLED);
    }
}
