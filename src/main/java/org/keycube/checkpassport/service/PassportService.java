package org.keycube.checkpassport.service;

import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycube.checkpassport.model.Passport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PassportService {

    @Inject
    Logger log;

    @ConfigProperty(name = "passports.expired.list.url")
    String fileUrl;

    @ConfigProperty(name = "passports.expired.list.path")
    String filePath;

    @ConfigProperty(name = "passports.expired.list.file.name")
    String fileName;

    @ConfigProperty(name = "passports.expired.list.file.archive.type")
    String archiveType;

    public void download() {
        log.info("Download file started");
        try {
            URL url = new URL(fileUrl + "/" + fileName + archiveType);
            File file = new File(filePath + File.separator + fileName + archiveType);
            FileUtils.copyURLToFile(url, file);
            log.info("Download finished");
        } catch (Exception e) {
            String error = "Download fails with error: " + e.getMessage();
            log.error(error);
            throw new WebApplicationException(error);
        }
    }

    public void decompress() {
        log.info("Decompress file started");
       try (BZip2CompressorInputStream is = new BZip2CompressorInputStream( new FileInputStream(
               filePath + File.separator + fileName + archiveType));
                FileOutputStream os = new FileOutputStream(filePath + File.separator + fileName)) {
           IOUtils.copyLarge(is, os);
           log.info("Decompress finished");
       } catch (Exception e) {
           String error = "Decompress fails with error: " + e.getMessage();
           log.error(error);
           throw new WebApplicationException(error);
       }
    }

    @Transactional
    public LocalDateTime persist(boolean test, boolean checkExistance) {
        log.info("Persist file started");
        try {
            File file;
            if (test) {
                URL fileUrl = PassportService.class.getClassLoader().getResource("exp_pass_test.csv");
                file = new File(fileUrl.getFile());
            }
            else {
                file = new File(filePath + File.separator + fileName);
            }
            CsvToBeanBuilder<Passport> builder = new CsvToBeanBuilder<>(new FileReader(file, StandardCharsets.UTF_8));
            List<Passport> list = builder
                    .withType(Passport.class)
                    .withSkipLines(1)
                    .build().parse();
            LocalDateTime date = LocalDateTime.now();
            list.forEach( passport -> {
                if (!checkExistance || !Passport.checkExpired(passport.serial, passport.number)) {
                    passport.date = date;
                    passport.persist();
                }
            });
            log.info("Persist finished");
            return date;
        } catch (Exception e) {
            String error = "Persist fails with error: " + e.getMessage();
            log.error(error);
            throw new WebApplicationException(error);
        }
    }
}
