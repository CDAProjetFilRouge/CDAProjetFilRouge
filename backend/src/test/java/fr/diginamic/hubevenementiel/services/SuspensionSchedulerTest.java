package fr.diginamic.hubevenementiel.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuspensionSchedulerTest {

    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private SuspensionScheduler suspensionScheduler;

    @Test
    void reactivateExpiredSuspensions_delegatesToTheService() {
        when(appUserService.reactivateExpiredSuspensions()).thenReturn(3);

        suspensionScheduler.reactivateExpiredSuspensions();

        verify(appUserService).reactivateExpiredSuspensions();
    }

    @Test
    void reactivateExpiredSuspensions_nothingToReactivate_doesNotFail() {
        when(appUserService.reactivateExpiredSuspensions()).thenReturn(0);

        assertThatCode(() -> suspensionScheduler.reactivateExpiredSuspensions()).doesNotThrowAnyException();
    }

    @Test
    void reactivateExpiredSuspensions_isScheduledEveryHour() throws NoSuchMethodException {
        Scheduled scheduled = SuspensionScheduler.class
                .getMethod("reactivateExpiredSuspensions")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 * * * *");
    }
}
