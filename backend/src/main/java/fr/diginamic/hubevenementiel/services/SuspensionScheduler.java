package fr.diginamic.hubevenementiel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SuspensionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SuspensionScheduler.class);

    private final AppUserService appUserService;

    public SuspensionScheduler(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * Every hour, reactivates the accounts whose suspension has expired (RG11).
     */
    @Scheduled(cron = "0 0 * * * *")
    public void reactivateExpiredSuspensions() {
        int count = appUserService.reactivateExpiredSuspensions();

        if (count > 0) {
            log.info("{} compte(s) réactivé(s) automatiquement après expiration de la suspension.", count);
        }
    }
}
