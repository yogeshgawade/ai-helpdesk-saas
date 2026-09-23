package com.helpdesk.sla.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlaBreachScheduler {

    private final SlaBreachService slaBreachService;

    public SlaBreachScheduler(
            SlaBreachService slaBreachService
    ) {
        this.slaBreachService = slaBreachService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void checkForSlaBreaches() {
        slaBreachService.checkForBreaches();
    }
}
