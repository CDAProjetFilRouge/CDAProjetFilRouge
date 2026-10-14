package fr.diginamic.hubevenementiel.services;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.dtos.dashboard.KpiResponseDto;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;

@Service
public class DashboardService {

    private final EventRepo eventRepo;
    private final InscriptionRepo inscriptionRepo;
    private final ClubRepo clubRepo;
    private final UserRepo userRepo;
    private final AnonymizationDemandRepo anonymizationDemandRepo;

    public DashboardService(EventRepo eventRepo, InscriptionRepo inscriptionRepo, ClubRepo clubRepo,
            UserRepo userRepo, AnonymizationDemandRepo anonymizationDemandRepo) {
        this.eventRepo = eventRepo;
        this.inscriptionRepo = inscriptionRepo;
        this.clubRepo = clubRepo;
        this.userRepo = userRepo;
        this.anonymizationDemandRepo = anonymizationDemandRepo;
    }

    /**
     *
     * @param principal the connected user
     * @return the indicators matching the role of the connected user
     */
    public List<KpiResponseDto> getKpis(AppUserPrincipal principal) {
        Role role = Role.valueOf(principal.role());
        LocalDateTime now = LocalDateTime.now();
        return switch (role) {
            case MEMBER -> memberKpis(principal.id(), now);
            case ORGANIZER -> organizerKpis(principal.id(), now);
            case ADMINISTRATOR -> administratorKpis(now);
        };
    }

    private List<KpiResponseDto> memberKpis(UUID userId, LocalDateTime now) {
        return List.of(
                new KpiResponseDto("MY_UPCOMING_EVENTS", "Événements à venir",
                        inscriptionRepo.countByUserIdAndStatusAndEventStartDateTimeGreaterThanEqual(
                                userId, InscriptionStatus.CONFIRMED, now)),
                new KpiResponseDto("MY_WAITING_LIST", "En liste d'attente",
                        inscriptionRepo.countByUserIdAndStatusAndEventStartDateTimeGreaterThanEqual(
                                userId, InscriptionStatus.WAITING_LIST, now)),
                new KpiResponseDto("MY_PAST_EVENTS", "Événements passés",
                        inscriptionRepo.countByUserIdAndStatusAndEventStartDateTimeLessThan(
                                userId, InscriptionStatus.CONFIRMED, now)),
                new KpiResponseDto("MY_CLUBS", "Mes clubs", clubRepo.countActiveByMemberId(userId)));
    }

    private List<KpiResponseDto> organizerKpis(UUID organizerId, LocalDateTime now) {
        long confirmed = inscriptionRepo.countForOrganizerEvents(
                organizerId, EventStatus.PUBLISHED, InscriptionStatus.CONFIRMED, now);
        long capacity = eventRepo.sumCapacityByOrganizer(organizerId, EventStatus.PUBLISHED, now);
        long fillRate = capacity == 0 ? 0 : Math.round(confirmed * 100.0 / capacity);

        return List.of(
                new KpiResponseDto("ORGANIZED_UPCOMING", "Événements à venir",
                        eventRepo.countByOrganizerIdAndStatusAndStartDateTimeGreaterThanEqual(
                                organizerId, EventStatus.PUBLISHED, now)),
                new KpiResponseDto("ORGANIZED_DRAFTS", "Brouillons",
                        eventRepo.countByOrganizerIdAndStatus(organizerId, EventStatus.DRAFT)),
                new KpiResponseDto("ORGANIZED_REGISTRATIONS", "Inscrits confirmés", confirmed),
                new KpiResponseDto("ORGANIZED_FILL_RATE", "Taux de remplissage (%)", fillRate));
    }

    private List<KpiResponseDto> administratorKpis(LocalDateTime now) {
        return List.of(
                new KpiResponseDto("ACTIVE_ACCOUNTS", "Comptes actifs",
                        userRepo.countByStatus(AccountStatus.ACTIVE)),
                new KpiResponseDto("SUSPENDED_ACCOUNTS", "Comptes suspendus",
                        userRepo.countByStatus(AccountStatus.SUSPENDED)),
                new KpiResponseDto("PENDING_ANONYMIZATIONS", "Demandes d'anonymisation en attente",
                        anonymizationDemandRepo.countByRequestStatus(RequestStatus.PENDING)),
                new KpiResponseDto("ACTIVE_CLUBS", "Clubs affiliés", clubRepo.countByEndValidityDateIsNull()),
                new KpiResponseDto("UPCOMING_EVENTS", "Événements à venir",
                        eventRepo.countByStatusAndStartDateTimeGreaterThanEqual(EventStatus.PUBLISHED, now)));
    }
}
