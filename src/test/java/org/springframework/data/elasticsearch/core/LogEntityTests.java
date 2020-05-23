/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexInitializer;
import org.springframework.test.context.ContextConfiguration;

/**
 * LogEntityTests
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
@ContextConfiguration(classes = { LogEntityTests.Config.class })
public class LogEntityTests {

	@Configuration
	@Import({ ElasticsearchRestTemplateConfiguration.class })
	static class Config {}

	private final IndexCoordinates index = IndexCoordinates.of("test-index-log-core");
	@Autowired private ElasticsearchOperations operations;
	private IndexOperations indexOperations;

	@BeforeEach
	public void before() throws ParseException {
		indexOperations = operations.indexOps(LogEntity.class);
		IndexInitializer.init(indexOperations);

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		IndexQuery indexQuery1 = new LogEntityBuilder("1").action("update").date(dateFormatter.parse("2013-10-18 18:01"))
				.code(2).ip("10.10.10.1").buildIndex();

		IndexQuery indexQuery2 = new LogEntityBuilder("2").action("update").date(dateFormatter.parse("2013-10-19 18:02"))
				.code(2).ip("10.10.10.2").buildIndex();

		IndexQuery indexQuery3 = new LogEntityBuilder("3").action("update").date(dateFormatter.parse("2013-10-19 18:03"))
				.code(2).ip("10.10.10.3").buildIndex();

		IndexQuery indexQuery4 = new LogEntityBuilder("4").action("update").date(dateFormatter.parse("2013-10-19 18:04"))
				.code(2).ip("10.10.10.4").buildIndex();

		operations.bulkIndex(Arrays.asList(indexQuery1, indexQuery2, indexQuery3, indexQuery4), index);
		indexOperations.refresh();
	}

	@AfterEach
	void after() {
		indexOperations.delete();
	}

	@Test // DATAES-66
	public void shouldIndexGivenLogEntityWithIPFieldType() {

		// when
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("ip", "10.10.10.1")).build();
		SearchHits<LogEntity> entities = operations.search(searchQuery, LogEntity.class, index);

		// then
		assertThat(entities).isNotNull().hasSize(1);
	}

	protected Class<? extends Exception> invalidIpExceptionClass() {
		return DataAccessException.class;
	}

	@Test // DATAES-66
	public void shouldThrowExceptionWhenInvalidIPGivenForSearchQuery() {

		// when
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(termQuery("ip", "10.10.10")).build();

		assertThatThrownBy(() -> {
			SearchHits<LogEntity> entities = operations.search(searchQuery, LogEntity.class, index);
		}).isInstanceOf(invalidIpExceptionClass());
	}

	@Test // DATAES-66
	public void shouldReturnLogsForGivenIPRanges() {

		// when
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(rangeQuery("ip").from("10.10.10.1").to("10.10.10.3")).build();
		SearchHits<LogEntity> entities = operations.search(searchQuery, LogEntity.class, index);

		// then
		assertThat(entities).isNotNull().hasSize(3);
	}

	/**
	 * Simple type to test facets
	 */
	@Data
	@Document(indexName = "test-index-log-core", replicas = 0, refreshInterval = "-1")
	static class LogEntity {

		private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		@Id private String id;

		private String action;

		private long sequenceCode;

		@Field(type = Ip) private String ip;

		@Field(type = Date, format = DateFormat.date_time) private java.util.Date date;

		private LogEntity() {}

		public LogEntity(String id) {
			this.id = id;
		}

	}

	/**
	 * Simple type to test facets
	 *
	 * @author Artur Konczak
	 * @author Mohsin Husen
	 */
	static class LogEntityBuilder {

		private LogEntity result;

		public LogEntityBuilder(String id) {
			result = new LogEntity(id);
		}

		public LogEntityBuilder action(String action) {
			result.setAction(action);
			return this;
		}

		public LogEntityBuilder code(long sequenceCode) {
			result.setSequenceCode(sequenceCode);
			return this;
		}

		public LogEntityBuilder date(Date date) {
			result.setDate(date);
			return this;
		}

		public LogEntityBuilder ip(String ip) {
			result.setIp(ip);
			return this;
		}

		public LogEntity build() {
			return result;
		}

		public IndexQuery buildIndex() {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(result.getId());
			indexQuery.setObject(result);
			return indexQuery;
		}
	}
}
