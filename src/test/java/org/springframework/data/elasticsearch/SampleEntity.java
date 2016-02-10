/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.elasticsearch;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.ScriptedField;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
@Document(indexName = "test-index", type = "test-type", shards = 1, replicas = 0, refreshInterval = "-1")
public class SampleEntity {

	@Id
	private String id;
	private String type;
	private String message;
	private int rate;
    @ScriptedField
    private Long scriptedRate;
	private boolean available;
	private String highlightedMessage;
	@Version
	private Long version;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}

    public Long getScriptedRate() {
        return scriptedRate;
    }

    public void setScriptedRate(Long scriptedRate) {
        this.scriptedRate = scriptedRate;
    }

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public String getHighlightedMessage() {
		return highlightedMessage;
	}

	public void setHighlightedMessage(String highlightedMessage) {
		this.highlightedMessage = highlightedMessage;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SampleEntity)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		SampleEntity rhs = (SampleEntity) obj;
		return new EqualsBuilder().append(this.id, rhs.id).append(this.type, rhs.type).append(this.message, rhs.message)
				.append(this.rate, rhs.rate).append(this.available, rhs.available).append(this.version, rhs.version).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).append(type).append(message).append(rate).append(available).append(version)
				.toHashCode();
	}

	@Override
	public String toString() {
		return "SampleEntity{" +
				"id='" + id + '\'' +
				", type='" + type + '\'' +
				", message='" + message + '\'' +
				", rate=" + rate +
				", available=" + available +
				", highlightedMessage='" + highlightedMessage + '\'' +
				", version=" + version +
				'}';
	}
}
