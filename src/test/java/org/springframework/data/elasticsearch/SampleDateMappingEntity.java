package org.springframework.data.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.Date;

import static org.springframework.data.elasticsearch.annotations.FieldIndex.not_analyzed;
import static org.springframework.data.elasticsearch.annotations.FieldType.String;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;

/**
 * @author Jakub Vavrik
 */
@Document(indexName = "test-datemapping", type = "mapping", indexStoreType = "memory", shards = 1, replicas = 0, refreshInterval = "-1")
public class SampleDateMappingEntity {
    @Id
    private String id;

    @Field(type = String, index = not_analyzed, store = true, searchAnalyzer = "standard", indexAnalyzer = "standard")
    private String message;

    @Field(type = Date, format = DateFormat.custom, pattern = "dd.MM.yyyy hh:mm")
    private Date customFormatDate;

    @Field(type = Date)
    private Date defaultFormatDate;

    @Field(type = Date, format = DateFormat.basic_date)
    private Date basicFormatDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCustomFormatDate() {
        return customFormatDate;
    }

    public void setCustomFormatDate(Date customFormatDate) {
        this.customFormatDate = customFormatDate;
    }

    public Date getDefaultFormatDate() {
        return defaultFormatDate;
    }

    public void setDefaultFormatDate(Date defaultFormatDate) {
        this.defaultFormatDate = defaultFormatDate;
    }

    public Date getBasicFormatDate() {
        return basicFormatDate;
    }

    public void setBasicFormatDate(Date basicFormatDate) {
        this.basicFormatDate = basicFormatDate;
    }
}
