package org.keycube.checkpassport.model;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

@Entity
@BatchSize(size = 1000)
@Schema(name = "Passport", description = "Данные паспорта", hidden = true)
public class Passport extends PanacheEntity {

    @NotEmpty
    @CsvBindByPosition(position = 0)
    @Schema(description = "Серия")
    public String serial;

    @NotEmpty
    @CsvBindByPosition(position = 1)
    @Schema(description = "Номер")
    public String number;

    @CsvIgnore
    @Schema(hidden = true)
    public LocalDateTime date;

    public static boolean checkExpired(String serial, String number) {
        return Passport.find("serial = :serial and number = :number",
                Parameters.with("serial", serial)
                        .and("number", number))
                .count() > 0;
    }

    @Transactional
    public static long deleteBeforeDate(LocalDateTime date) {
        return Passport.delete("date < :date",
                Parameters.with("date", date));
    }
}
