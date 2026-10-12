package fr.diginamic.hubevenementiel.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.diginamic.hubevenementiel.dtos.dashboard.KpiResponseDto;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EventRepo eventRepo;
    @Mock
    private InscriptionRepo inscriptionRepo;
    @Mock
    private ClubRepo clubRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private AnonymizationDemandRepo anonymizationDemandRepo;

    @InjectMocks
    private DashboardService dashboardService;

    private long valueOf(List<KpiResponseDto> kpis, String code) {
        return kpis.stream().filter(k -> k.code().equals(code)).findFirst().orElseThrow().value();
    }

    @Test
    void getKpis_member_returnsInscriptionAndClubCounters() {
        AppUserPrincipal principal = new AppUserPrincipal(4L, "m@example.com", "MEMBER");
        when(inscriptionRepo.countByUserIdAndStatusAndEventStartDateTimeGreaterThanEqual(
                eq(4L), eq(InscriptionStatus.CONFIRMED), any(LocalDateTime.class))).thenReturn(3L);
        when(inscriptionRepo.countByUserIdAndStatusAndEventStartDateTimeGreaterThanEqual(
                eq(4L), eq(InscriptionStatus.WAITING_LIST), any(LocalDateTime.class))).thenReturn(1L);
        when(inscriptionRepo.countByUserIdAndStatusAndEventStartDateTimeLessThan(
                eq(4L), eq(InscriptionStatus.CONFIRMED), any(LocalDateTime.class))).thenReturn(7L);
        when(clubRepo.countActiveByMemberId(4L)).thenReturn(2L);

        List<KpiResponseDto> kpis = dashboardService.getKpis(principal);

        assertThat(kpis).hasSize(4);
        assertThat(valueOf(kpis, "MY_UPCOMING_EVENTS")).isEqualTo(3L);
        assertThat(valueOf(kpis, "MY_WAITING_LIST")).isEqualTo(1L);
        assertThat(valueOf(kpis, "MY_PAST_EVENTS")).isEqualTo(7L);
        assertThat(valueOf(kpis, "MY_CLUBS")).isEqualTo(2L);
    }

    @Test
    void getKpis_organizer_computesFillRate() {
        AppUserPrincipal principal = new AppUserPrincipal(5L, "o@example.com", "ORGANIZER");
        when(inscriptionRepo.countForOrganizerEvents(eq(5L), eq(EventStatus.PUBLISHED),
                eq(InscriptionStatus.CONFIRMED), any(LocalDateTime.class))).thenReturn(30L);
        when(eventRepo.sumCapacityByOrganizer(eq(5L), eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(120L);
        when(eventRepo.countByOrganizerIdAndStatusAndStartDateTimeGreaterThanEqual(
                eq(5L), eq(EventStatus.PUBLISHED), any(LocalDateTime.class))).thenReturn(4L);
        when(eventRepo.countByOrganizerIdAndStatus(5L, EventStatus.DRAFT)).thenReturn(2L);

        List<KpiResponseDto> kpis = dashboardService.getKpis(principal);

        assertThat(valueOf(kpis, "ORGANIZED_UPCOMING")).isEqualTo(4L);
        assertThat(valueOf(kpis, "ORGANIZED_DRAFTS")).isEqualTo(2L);
        assertThat(valueOf(kpis, "ORGANIZED_REGISTRATIONS")).isEqualTo(30L);
        assertThat(valueOf(kpis, "ORGANIZED_FILL_RATE")).isEqualTo(25L);
    }

    @Test
    void getKpis_organizerWithoutCapacity_fillRateIsZero() {
        AppUserPrincipal principal = new AppUserPrincipal(5L, "o@example.com", "ORGANIZER");

        List<KpiResponseDto> kpis = dashboardService.getKpis(principal);

        assertThat(valueOf(kpis, "ORGANIZED_FILL_RATE")).isZero();
    }

    @Test
    void getKpis_administrator_returnsGlobalCounters() {
        AppUserPrincipal principal = new AppUserPrincipal(1L, "a@example.com", "ADMINISTRATOR");
        when(userRepo.countByStatus(AccountStatus.ACTIVE)).thenReturn(40L);
        when(userRepo.countByStatus(AccountStatus.SUSPENDED)).thenReturn(3L);
        when(anonymizationDemandRepo.countByRequestStatus(RequestStatus.PENDING)).thenReturn(5L);
        when(clubRepo.countByEndValidityDateIsNull()).thenReturn(8L);
        when(eventRepo.countByStatusAndStartDateTimeGreaterThanEqual(
                eq(EventStatus.PUBLISHED), any(LocalDateTime.class))).thenReturn(12L);

        List<KpiResponseDto> kpis = dashboardService.getKpis(principal);

        assertThat(kpis).hasSize(5);
        assertThat(valueOf(kpis, "ACTIVE_ACCOUNTS")).isEqualTo(40L);
        assertThat(valueOf(kpis, "SUSPENDED_ACCOUNTS")).isEqualTo(3L);
        assertThat(valueOf(kpis, "PENDING_ANONYMIZATIONS")).isEqualTo(5L);
        assertThat(valueOf(kpis, "ACTIVE_CLUBS")).isEqualTo(8L);
        assertThat(valueOf(kpis, "UPCOMING_EVENTS")).isEqualTo(12L);
    }
}
