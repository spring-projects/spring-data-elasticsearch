/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.facet;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * Simple type to test facets
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 */

@Document(indexName = "test-index-log", type = "test-log-type", shards = 1, replicas = 0, refreshInterval = "-1")
public class LogEntity {

	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

	@Id
	private String id;

	private String action;

	private long sequenceCode;

	@Field(type = Ip)
	private String ip;

	@Field(type = Date)
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

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
