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
package org.springframework.data.elasticsearch.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Chris White
 * @author Sascha Woo
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Document(indexName = "test-index-sample", type = "test-type", shards = 1, replicas = 0, refreshInterval = "-1")
public class SampleEntity {

	@Id
	private String id;
	@Field(type = Text, store = true, fielddata = true)
	private String type;
	@Field(type = Text, store = true, fielddata = true)
	private String message;
	private int rate;
	@ScriptedField
	private Double scriptedRate;
	private boolean available;
	private String highlightedMessage;
	private GeoPoint location;
	@Version
	private Long version;
	@Score
	private float score;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SampleEntity that = (SampleEntity) o;

		if (available != that.available) return false;
		if (rate != that.rate) return false;
		if (highlightedMessage != null ? !highlightedMessage.equals(that.highlightedMessage) : that.highlightedMessage != null)
			return false;
		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (location != null ? !location.equals(that.location) : that.location != null) return false;
		if (message != null ? !message.equals(that.message) : that.message != null) return false;
		if (type != null ? !type.equals(that.type) : that.type != null) return false;
		if (version != null ? !version.equals(that.version) : that.version != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (message != null ? message.hashCode() : 0);
		result = 31 * result + rate;
		result = 31 * result + (available ? 1 : 0);
		result = 31 * result + (highlightedMessage != null ? highlightedMessage.hashCode() : 0);
		result = 31 * result + (location != null ? location.hashCode() : 0);
		result = 31 * result + (version != null ? version.hashCode() : 0);
		return result;
	}
}
