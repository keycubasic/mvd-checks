package org.keycube.checkpassport.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jboss.logging.Logger;
import org.keycube.checkpassport.model.Passport;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class PassportService {

    @Inject
    Logger log;

    @Inject
    EntityManager manager;

    @Inject
    SearchSession searchSession;

    @ConfigProperty(name = "passports.expired.list.file.url")
    String fileUrl;

    @ConfigProperty(name = "passports.expired.list.file.path")
    String filePath;

    @ConfigProperty(name = "passports.expired.list.file.name")
    String fileName;

    @ConfigProperty(name = "passports.expired.list.file.archive.type")
    String archiveType;

    @ConfigProperty(name = "passports.expired.list.split.path")
    String splitPath;

    @ConfigProperty(name = "passports.expired.list.split.size", defaultValue = "100000000") // 95MB
    long bytesPerSplit;

    @ConfigProperty(name = "passports.expired.gc.timeout", defaultValue = "10000") // 10s
    long gcTimeout;

    @ConfigProperty(name = "mass-index.on-startup")
    boolean massIndex;

    AtomicInteger counter = new AtomicInteger();

    @Transactional
    void onStart(@Observes StartupEvent ev) throws InterruptedException {
        if (massIndex && Passport.count() > 0) {
            searchSession.massIndexer()
                    .startAndWait();
        }
    }

    // TODO: refactor with apache-camel
    public void download() {
        log.info("download file started");
        try {
            URL url = new URL(fileUrl + "/" + fileName + archiveType);
            File file = new File(filePath + File.separator + fileName + archiveType);
            FileUtils.copyURLToFile(url, file);
            log.info("download finished");
        } catch (Exception e) {
            String error = "download fails with error: " + e.getMessage();
            log.error(error);
            throw new WebApplicationException(error);
        } finally {
            gc();
        }
    }

    public void decompress() {
        log.info("decompress file started");
        try (BZip2CompressorInputStream is = new BZip2CompressorInputStream(new FileInputStream(
                filePath + File.separator + fileName + archiveType));
             FileOutputStream os = new FileOutputStream(filePath + File.separator + fileName)) {
            IOUtils.copyLarge(is, os);
            log.info("decompress finished");
        } catch (Exception e) {
            String error = "decompress fails with error: " + e.getMessage();
            log.error(error);
            throw new WebApplicationException(error);
        } finally {
            gc();
        }
    }

    public void split() {
        log.info("split file started");
        try (RandomAccessFile raf = new RandomAccessFile(filePath + File.separator + fileName, "r")) {
            long sourceSize = raf.length();
            long numSplits = sourceSize / bytesPerSplit;
            long remainingBytes = sourceSize % numSplits;
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"));
            int maxReadBufferSize = 8 * 1024; //8KB
            for (int destIx = 1; destIx <= numSplits; destIx++) {
                String postfix = String.format("%03d", destIx);
                BufferedOutputStream bw = new BufferedOutputStream(
                        new FileOutputStream(splitPath + File.separator + fileName + "_" + date + "_" + postfix));
                if (bytesPerSplit > maxReadBufferSize) {
                    long numReads = bytesPerSplit / maxReadBufferSize;
                    long numRemainingRead = bytesPerSplit % maxReadBufferSize;
                    for (int i = 0; i < numReads; i++) {
                        readWrite(raf, bw, maxReadBufferSize);
                    }
                    if (numRemainingRead > 0) {
                        readWrite(raf, bw, numRemainingRead);
                    }
                } else {
                    readWrite(raf, bw, bytesPerSplit);
                }
                bw.close();
            }
            if (remainingBytes > 0) {
                String postfix = String.format("%03d", numSplits + 1);
                BufferedOutputStream bw = new BufferedOutputStream(
                        new FileOutputStream(splitPath + File.separator + fileName + "_" + date + "_" + postfix));
                readWrite(raf, bw, remainingBytes);
                bw.close();
            }
            log.info("split file finished");
        } catch (Exception e) {
            String error = "split fails with error: " + e.getMessage();
            log.error(error);
            throw new WebApplicationException(error);
        } finally {
            gc();
        }
    }

    private static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if (val != -1) {
            bw.write(buf);
        }
    }

    @Transactional
    public LocalDate persist(boolean test) {
        log.info("bind file started");
        try {
            LocalDateTime date = LocalDateTime.now();
            File file;
            if (test) {
                URL fileUrl = PassportService.class.getClassLoader().getResource("csv/exp_pass_test.csv");
                file = new File(fileUrl.getFile());
            } else {
                List<String> fileNameList = listFileNameInDir(splitPath);
                if (fileNameList.size() > 0) {
                    file = new File(splitPath + File.separator + fileNameList.get(0));
                } else {
                    log.info("no files to bind");
                    return date.toLocalDate();
                }
            }
            String fileName = file.getName();
            log.info(file.getName());
            FileReader reader = new FileReader(file, StandardCharsets.UTF_8);
            CsvToBeanBuilder<Passport> builder = new CsvToBeanBuilder<>(reader);
            CsvToBean<Passport> csvToBean = builder
                    .withType(Passport.class)
                    // .withSkipLines(1)
                    .withIgnoreEmptyLine(true)
                    .build();
            // .parse(); // OOM Java heap space
            counter.set(0);
            csvToBean.forEach(passport -> {
                int i = counter.incrementAndGet();
                try {
                    passport.date = date;
                    passport.persist();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                if (i % 1000000 == 0) {
                    log.debug(i);
                }
            });
            reader.close();
            if (!test) {
                boolean deleted = file.delete();
                log.info("file \"" + fileName + (deleted ? " " : " not") + "\" deleted");
            }
            log.info("bind finished");
            return date.toLocalDate();
        } catch (Exception e) {
            String error = "bind fails with error: " + e.getMessage();
            log.error(error);
            throw new WebApplicationException(error);
        } finally {
            gc();
        }
    }

    public boolean checkByElastic(String serial, String number) {
        long start = System.currentTimeMillis();
        boolean present = searchSession.search(Passport.class)
                .where(f -> f.bool()
                        .must(
                                f.match().field("serial")
                                        .matching(serial))
                        .must(
                                f.match().field("number")
                                        .matching(number))
                ).fetchSingleHit().isPresent();
        long finish = System.currentTimeMillis();
        log.info("checkByElastic duration: " + (finish - start));
        return present;
    }

    public boolean checkByPostgres(String serial, String number) {
        long start = System.currentTimeMillis();
        boolean present = (int) manager.createNativeQuery(
                        "select * from isPresent(:serial, :number)")
                .setParameter("serial", serial)
                .setParameter("number", number)
                .getSingleResult() > 0;
        long finish = System.currentTimeMillis();
        log.info("checkByPostgres duration: " + (finish - start));
        return present;
    }

    private List<String> listFileNameInDir(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public void gc() {
        try {
            System.gc();
            Thread.sleep(gcTimeout);
        } catch (Exception ignored) {
        }
    }

}
