package org.keycube.checkpassport.model;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.BatchSize;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Indexed
@BatchSize(size = 1000)
@Schema(name = "Passport", description = "Данные паспорта", hidden = true)
public class Passport extends PanacheEntity {

    @KeywordField
    @CsvBindByPosition(position = 0)
    @Schema(description = "Серия")
    public String serial;

    @KeywordField
    @CsvBindByPosition(position = 1)
    @Schema(description = "Номер")
    public String number;

    @CsvIgnore
    @Schema(hidden = true)
    public LocalDateTime date;

    public static boolean checkByHibernate(String serial, String number) {
        return Passport.find("serial = :serial and number = :number",
                        Parameters.with("serial", serial)
                                .and("number", number))
                .count() > 0;
    }

    @Transactional
    public static long deleteBeforeDate(LocalDate date) {
        return Passport.delete("date < :date",
                Parameters.with("date", date.atStartOfDay()));
    }
}
