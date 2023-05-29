package org.keycube.checkpassport.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.keycube.checkpassport.model.Passport;
import org.keycube.checkpassport.service.PassportService;

import java.time.LocalDateTime;

@Singleton
public class PassportScheduler {

    @Inject
    Logger log;

    @Inject
    PassportService service;

    @Scheduled(cron = "{append.cron.expr}")
    void append() throws InterruptedException {
        log.info("append scheduled");
        service.download();
        Thread.sleep(10000);
        service.decompress();
        Thread.sleep(10000);
        service.persist(false, true);
        log.info("append scheduler finished");
    }

    @Scheduled(cron = "{refresh.cron.expr}")
    void refresh() throws InterruptedException {
        log.info("refresh scheduled");
        service.download();
        Thread.sleep(10000);
        service.decompress();
        Thread.sleep(10000);
        LocalDateTime date = service.persist(false, false);
        Passport.deleteBeforeDate(date);
        log.info("refresh scheduler finished");
    }

    @Scheduled(cron = "{persistTestNew.cron.expr}")
    void persistTestNew() {
        log.info("persistTestNew scheduled");
        service.persist(true, true);
        log.info("persistTestNew scheduler finished");
    }

    @Scheduled(cron = "{persistTestAll.cron.expr}")
    void persistTestAll() {
        log.info("persistTestAll scheduled");
        LocalDateTime date = service.persist(true, false);
        Passport.deleteBeforeDate(date);
        log.info("persistTestAll scheduler finished");
    }
}