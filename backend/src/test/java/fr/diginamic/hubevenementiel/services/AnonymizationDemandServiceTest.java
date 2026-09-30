package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymizationDemandServiceTest {

    @Mock
    private AnonymizationDemandRepo anonymizationDemandRepository;
    @Mock
    private AppUserService appUserService;
    @Mock
    private AddressService addressService;

    @InjectMocks
    private AnonymizationDemandService anonymizationDemandService;

    private AppUser requester;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        requester = new AppUser();
        requester.setId(6L);

        admin = new AppUser();
        admin.setId(9L);
    }

    // ---------------------------------------------------------------
    // request
    // ---------------------------------------------------------------

    @Test
    void request_unknownUser_throwsNotFound() throws HttpException {
        AppUserPrincipal principal = new AppUserPrincipal(999L, "inconnu@example.com", "MEMBER");
        when(appUserService.findById(999L)).thenThrow(new NotFoundException("User not found with id: 999"));

        assertThrows(NotFoundException.class, () -> anonymizationDemandService.request(principal));
    }

    @Test
    void request_createsDemandForTheCallerHimself() throws HttpException {
        AppUserPrincipal principal = new AppUserPrincipal(6L, "alice@example.com", "MEMBER");
        when(appUserService.findById(6L)).thenReturn(requester);
        when(anonymizationDemandRepository.save(any(AnonymizationDemand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnonymizationDemand result = anonymizationDemandService.request(principal);

        assertThat(result.getRequester()).isEqualTo(requester);
    }

    // ---------------------------------------------------------------
    // validate
    // ---------------------------------------------------------------

    @Test
    void validate_unknownDemandId_throwsNotFound() throws HttpException {
        AppUserPrincipal principal = new AppUserPrincipal(9L, "emma@example.com", "ADMINISTRATOR");
        when(appUserService.findById(9L)).thenReturn(admin);
        when(anonymizationDemandRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> anonymizationDemandService.validate(1L, principal));
    }

    @Test
    void validate_happyPath_setsAdminApprovedDateAndStatusValidate() throws HttpException {
        AppUserPrincipal principal = new AppUserPrincipal(9L, "emma@example.com", "ADMINISTRATOR");
        AnonymizationDemand demand = new AnonymizationDemand();
        demand.setId(1L);
        demand.setRequester(requester);
        demand.setRequestStatus(RequestStatus.PENDING);

        when(appUserService.findById(9L)).thenReturn(admin);
        when(anonymizationDemandRepository.findById(1L)).thenReturn(Optional.of(demand));
        when(anonymizationDemandRepository.save(any(AnonymizationDemand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnonymizationDemand result = anonymizationDemandService.validate(1L, principal);

        assertThat(result.getRequestStatus()).isEqualTo(RequestStatus.VALIDATE);
        assertThat(result.getAdmin()).isEqualTo(admin);
        assertThat(result.getApprovedDate()).isNotNull();
    }

    @Test
    void validate_adminIsTheAuthenticatedCaller_notAClientSuppliedId() throws HttpException {
        AppUserPrincipal principal = new AppUserPrincipal(9L, "emma@example.com", "ADMINISTRATOR");
        AnonymizationDemand demand = new AnonymizationDemand();
        demand.setId(1L);
        demand.setRequester(requester);
        demand.setRequestStatus(RequestStatus.PENDING);

        when(appUserService.findById(9L)).thenReturn(admin);
        when(anonymizationDemandRepository.findById(1L)).thenReturn(Optional.of(demand));
        when(anonymizationDemandRepository.save(any(AnonymizationDemand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        anonymizationDemandService.validate(1L, principal);

        // appUserService.findById n'est appelé qu'avec l'id du principal (9L), jamais un id arbitraire
        org.mockito.Mockito.verify(appUserService).findById(9L);
    }

    // ---------------------------------------------------------------
    // anonymizationDemandChecker
    // ---------------------------------------------------------------

    @Test
    void anonymizationDemandChecker_nullDemand_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> anonymizationDemandService.anonymizationDemandChecker(null));
    }

    @Test
    void anonymizationDemandChecker_nullRequester_throwsBadRequest() {
        AnonymizationDemand demand = new AnonymizationDemand();
        demand.setRequester(null);

        assertThrows(BadRequestException.class, () -> anonymizationDemandService.anonymizationDemandChecker(demand));
    }

    @Test
    void anonymizationDemandChecker_unknownRequester_throwsNotFound() throws HttpException {
        AnonymizationDemand demand = new AnonymizationDemand();
        demand.setRequester(requester);
        when(appUserService.findById(6L)).thenThrow(new NotFoundException("User not found with id: 6"));

        assertThrows(NotFoundException.class, () -> anonymizationDemandService.anonymizationDemandChecker(demand));
    }

    // ---------------------------------------------------------------
    // createDemand
    // ---------------------------------------------------------------

    @Test
    void createDemand_forcesPendingStatusAndClearsAdminApprovedDate() throws HttpException {
        AnonymizationDemand demand = new AnonymizationDemand();
        demand.setRequester(requester);
        demand.setRequestStatus(RequestStatus.VALIDATE);
        demand.setAdmin(admin);
        demand.setApprovedDate(LocalDateTime.now());
        when(appUserService.findById(6L)).thenReturn(requester);
        when(anonymizationDemandRepository.save(any(AnonymizationDemand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnonymizationDemand result = anonymizationDemandService.createDemand(demand);

        assertThat(result.getRequestStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getAdmin()).isNull();
        assertThat(result.getApprovedDate()).isNull();
        assertThat(result.getDemandDate()).isNotNull();
    }

    // ---------------------------------------------------------------
    // recherches par date
    // ---------------------------------------------------------------

    @Test
    void findByDemandBetweenDates_startAfterEnd_throwsBadRequest() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now();

        assertThrows(BadRequestException.class,
                () -> anonymizationDemandService.findByDemandBetweenDates(0, 20, start, end));
    }

    @Test
    void findApprovedDemandBetweenDates_startAfterEnd_throwsBadRequest() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now();

        assertThrows(BadRequestException.class,
                () -> anonymizationDemandService.findApprovedDemandBetweenDates(0, 20, start, end));
    }
}
