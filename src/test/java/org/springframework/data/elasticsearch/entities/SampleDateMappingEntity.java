package org.springframework.data.elasticsearch.entities;

import static org.springframework.data.elasticsearch.annotations.FieldIndex.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.String;

import java.util.Date;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * @author Jakub Vavrik
 *
 * @See DATAES-287 - updated test entity that worked in ES < 2.0 with post 2.0 date formats
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(indexName = "test-datemapping", type = "mapping", shards = 1, replicas = 0, refreshInterval = "-1")
public class SampleDateMappingEntity {

	@Id
	private String id;

	@Field(type = String, index = not_analyzed, store = true, analyzer = "standard")
	private String message;

	@Field(type = Date, format = DateFormat.custom, pattern = "dd.MM.yyyy hh:mm")
	private Date customFormatDate;

	@Field(type = Date)
	private Date defaultFormatDate;

	@Field(type = Date, format = DateFormat.basic_date)
	private Date basicFormatDate;

	@Field(type = Date, format = DateFormat.epoch_millis)
	private Date epochMillisDate;
}
