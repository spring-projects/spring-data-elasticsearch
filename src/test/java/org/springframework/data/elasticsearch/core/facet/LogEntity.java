package org.springframework.data.elasticsearch.core.facet;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * User: Artur Konczak
 * Date: 18/10/13
 * Time: 17:33
 */
@Document(indexName = "logs", type = "log", shards = 1, replicas = 0, refreshInterval = "-1", indexStoreType = "memory")
public class LogEntity {

	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	@Id
	private String id;

	private String action;

	private long sequenceCode;

	@Field(type = FieldType.Date, format = DateFormat.basic_date_time)
	private Date date;

	private LogEntity() {
	}

	public LogEntity(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAction() {
		return action;
	}

	public String toString() {
		return new StringBuffer().append("{id:").append(id).append(",action:").append(action).append(",code:").append(sequenceCode).append(",date:").append(format.format(date)).append("}").toString();
	}

	public void setAction(String action) {
		this.action = action;
	}

	public long getSequenceCode() {
		return sequenceCode;
	}

	public void setSequenceCode(long sequenceCode) {
		this.sequenceCode = sequenceCode;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
