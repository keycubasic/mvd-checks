package org.keycube.checkpassport.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.keycube.checkpassport.model.Passport;
import org.keycube.checkpassport.service.PassportService;

import java.time.LocalDate;

@Singleton
public class PassportScheduler {

    @Inject
    Logger log;

    @Inject
    PassportService service;

    @Scheduled(cron = "{reload.cron.expr}")
    void reload() {
        log.info("reload scheduled");
        service.download();
        service.decompress();
        service.split();
        log.info("reload scheduler finished");
    }

    @Scheduled(cron = "{persist.cron.expr}")
    void persist() {
        log.info("persist scheduled");
        service.persist(false);
        log.info("persist scheduler finished");
    }

    @Scheduled(cron = "{delete.cron.expr}")
    void delete() {
        log.info("persist scheduled");
        LocalDate date = LocalDate.now();
        Passport.deleteBeforeDate(date);
        log.info("persist scheduler finished");
    }

    @Scheduled(cron = "{persistTest.cron.expr}")
    void persistTest() {
        log.info("persistTest scheduled");
        LocalDate date = service.persist(true);
        Passport.deleteBeforeDate(date);
        log.info("persistTest scheduler finished");
    }
}