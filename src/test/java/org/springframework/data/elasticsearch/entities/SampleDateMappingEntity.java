package org.springframework.data.elasticsearch.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * @author Jakub Vavrik
 */
@Document(indexName = "test-index-date-mapping", type = "mapping", shards = 1, replicas = 0, refreshInterval = "-1")
public class SampleDateMappingEntity {

	@Id
	private String id;

	@Field(type = Text, index = false, store = true, analyzer = "standard")
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
