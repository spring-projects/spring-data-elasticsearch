/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client.elc;

import static org.assertj.core.api.Assertions.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsRequest;
import co.elastic.clients.elasticsearch.indices.GetIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.RequestOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.support.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * Not really tests, but a class to check the first implementations of the new Elasticsearch client. Needs Elasticsearch
 * on port 9200 and an intercepting proxy on port 8080.
 *
 * @author Peter-Josef Meisch
 */
@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DevTests {

	private static final Log LOGGER = LogFactory.getLog(DevTests.class);

	private static final String INDEX = "appdata-index";

	private static final SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
	private static final MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);

	private final TransportOptions transportOptions = new RestClientOptions(RequestOptions.DEFAULT).toBuilder()
			.addHeader("X-SpringDataElasticsearch-AlwaysThere", "true").setParameter("pretty", "true").build();

	private final JsonpMapper jsonpMapper = new JacksonJsonpMapper();

	private final ReactiveElasticsearchClient reactiveElasticsearchClient = ElasticsearchClients
			.createReactive(clientConfiguration(), transportOptions);
	private final ElasticsearchClient imperativeElasticsearchClient = ElasticsearchClients
			.createImperative(ElasticsearchClients.getRestClient(clientConfiguration()), transportOptions, jsonpMapper);

	@Test
	void someTest() throws IOException {

		ElasticsearchClient client = imperativeElasticsearchClient;
		ElasticsearchIndicesClient indicesClient = client.indices();

		indicesClient.create(b -> b.index("testindex"));

		GetIndicesSettingsResponse getIndicesSettingsResponse = indicesClient
				.getSettings(GetIndicesSettingsRequest.of(b -> b.index("testindex").includeDefaults(true)));
	}

	static class ReactiveClient {
		private final ElasticsearchTransport transport;

		ReactiveClient(ElasticsearchTransport transport) {
			this.transport = transport;
		}

		public <T> Mono<IndexResponse> index(IndexRequest<T> request) {
			return Mono.fromFuture(transport.performRequestAsync(request, IndexRequest._ENDPOINT, null));
		}
	}

	static class Product {
		@Nullable String id;
		@Nullable Double price;

		public Product() {}

		public Product(@Nullable String id, @Nullable Double price) {
			this.id = id;
			this.price = price;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Double getPrice() {
			return price;
		}

		public void setPrice(@Nullable Double price) {
			this.price = price;
		}
	}

	static class Person {
		@Nullable String id;
		@Nullable Name name;

		public Person() {}

		public Person(String id, Name name) {
			this.id = id;
			this.name = name;
		}

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "Person{" + "id='" + id + '\'' + ", name=" + name + '}';
		}
	}

	static class Name {
		@Nullable String first;
		@Nullable String last;

		public Name() {}

		public Name(String first, String last) {
			this.first = first;
			this.last = last;
		}

		@Nullable
		public String getFirst() {
			return first;
		}

		public void setFirst(String first) {
			this.first = first;
		}

		@Nullable
		public String getLast() {
			return last;
		}

		public void setLast(String last) {
			this.last = last;
		}

		@Override
		public String toString() {
			return "Name{" + "first='" + first + '\'' + ", last='" + last + '\'' + '}';
		}
	}

	// region cluster health
	@Test
	@Order(10)
	void clusterHealth() {

		HealthRequest healthRequest = new HealthRequest.Builder().build();

		try {
			HealthResponse healthResponse = clusterHealthImperative(healthRequest);
		} catch (IOException e) {
			LOGGER.error("error", e);
		}

		try {
			HealthResponse healthResponse = clusterHealthReactive(healthRequest);
		} catch (IOException e) {
			LOGGER.error("error", e);
		}
	}

	private HealthResponse clusterHealthImperative(HealthRequest healthRequest) throws IOException {
		return imperativeElasticsearchClient.cluster().health(healthRequest);
	}

	private HealthResponse clusterHealthReactive(HealthRequest healthRequest) throws IOException {
		return Objects.requireNonNull(reactiveElasticsearchClient.cluster().health(healthRequest).block());
	}
	// endregion

	@Test
	@Order(15)
	void indexCreation() throws IOException {

		RequestConverter requestConverter = new RequestConverter(converter,
				imperativeElasticsearchClient._transport().jsonpMapper());

		String index = "pjtestindex";
		ElasticsearchIndicesClient indicesClient = imperativeElasticsearchClient.indices();

		if (indicesClient.exists(erb -> erb.index(index)).value()) {
			indicesClient.delete(drb -> drb.index(index));
		}

		String jsonSettings = """
				{
				    "index": {
				        "number_of_shards": "1",
				        "number_of_replicas": "0",
				        "analysis": {
				            "analyzer": {
				                "emailAnalyzer": {
				                    "type": "custom",
				                    "tokenizer": "uax_url_email"
				                }
				            }
				        }
				    }
				}
				""";

		String jsonMapping = """
				{
				  "properties": {
				    "email": {
				      "type": "text",
				      "analyzer": "emailAnalyzer"
				    }
				  }
				}
				""";

		indicesClient.create(crb -> crb //
				.index(index) //
				.settings(requestConverter.fromJson(jsonSettings, IndexSettings._DESERIALIZER)) //
				.mappings(requestConverter.fromJson(jsonMapping, TypeMapping._DESERIALIZER)));
	}

	// region save
	@Test
	@Order(20)
	void save() {

		Function<Integer, IndexRequest<AppData>> indexRequestBuilder = (Integer id) -> {
			AppData appData = new AppData();
			appData.setId("id" + id);
			appData.setContent("content" + id);

			return new IndexRequest.Builder<AppData>().id(appData.getId()).document(appData).index(INDEX).build();
		};

		try {
			indexImperative(indexRequestBuilder.apply(1));
		} catch (IOException e) {
			LOGGER.error("error", e);
		}
		try {
			indexReactive(indexRequestBuilder.apply(2));
		} catch (IOException e) {
			LOGGER.error("error", e);
		}

	}

	private IndexResponse indexImperative(IndexRequest<AppData> indexRequest) throws IOException {
		return imperativeElasticsearchClient.index(indexRequest);
	}

	private IndexResponse indexReactive(IndexRequest<AppData> indexRequest) throws IOException {
		return Objects.requireNonNull(reactiveElasticsearchClient.index(indexRequest).block());
	}

	// endregion
	// region search
	@Test
	@Order(30)
	void search() {

		SearchRequest searchRequest = new SearchRequest.Builder().index(INDEX)
				.query(query -> query.match(matchQuery -> matchQuery.field("content").query(FieldValue.of("content1"))))
				.build();

		ResponseBody<EntityAsMap> searchResponse = null;
		try {
			searchResponse = searchImperative(searchRequest);
			assertThat(searchResponse).isNotNull();
		} catch (IOException e) {
			LOGGER.error("error", e);
		}

		try {
			searchResponse = searchReactive(searchRequest);
			assertThat(searchResponse).isNotNull();
		} catch (Exception e) {
			LOGGER.error("error", e);
		}
	}

	private ResponseBody<EntityAsMap> searchImperative(SearchRequest searchRequest) throws IOException {
		return imperativeElasticsearchClient.search(searchRequest, EntityAsMap.class);
	}

	private ResponseBody<EntityAsMap> searchReactive(SearchRequest searchRequest) {
		return Objects.requireNonNull(reactiveElasticsearchClient.search(searchRequest, EntityAsMap.class).block());
	}
	// endregion

	private ClientConfiguration clientConfiguration() {
		return ClientConfiguration.builder() //
				.connectedTo("localhost:9200")//
				.withBasicAuth("elastic", "hcraescitsale").withProxy("localhost:8080") //
				.withHeaders(() -> {
					HttpHeaders headers = new HttpHeaders();
					headers.add("X-SpringDataElasticsearch-timestamp",
							LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
					return headers;
				}) //
				.build();
	}

	private static class AppData {
		@Nullable private String id;
		@Nullable private String content;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}
}
