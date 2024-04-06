/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.*;
import static io.specto.hoverfly.junit.verification.HoverflyVerifications.*;
import static org.assertj.core.api.Assertions.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import io.specto.hoverfly.junit5.api.HoverflyCapture;
import io.specto.hoverfly.junit5.api.HoverflyConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

/**
 * We need hoverfly for testing the reactive code to use a proxy. Wiremock cannot intercept the proxy calls as WebClient
 * uses HTTP CONNECT on proxy requests which wiremock does not support. <br/>
 * Note: since 5.0 we do not use the WebClient for the reactive code anymore, so this might be handled with two
 * wiremocks, but there is no real need to change this test setup.
 *
 * @author Peter-Josef Meisch
 */
@SuppressWarnings("UastIncorrectHttpHeaderInspection")
@HoverflyCapture(path = "target/hoverfly", config = @HoverflyConfig(proxyLocalHost = true, plainHttpTunneling = true))
@ExtendWith(HoverflyExtension.class)
public class RestClientsTest {

	@ParameterizedTest(name = "{0}") // DATAES-700
	@MethodSource("clientUnderTestFactorySource")
	@DisplayName("should use configured proxy")
	void shouldUseConfiguredProxy(ClientUnderTestFactory clientUnderTestFactory, Hoverfly hoverfly) throws IOException {

		wireMockServer(server -> {

			// wiremock is the dummy server, hoverfly the proxy

			String serviceHost = "localhost:" + server.port();
			String proxyHost = "localhost:" + hoverfly.getHoverflyConfig().getProxyPort();

			ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();
			ClientConfiguration clientConfiguration = configurationBuilder //
					.connectedTo(serviceHost)//
					.withProxy(proxyHost) //
					.build();
			ClientUnderTest clientUnderTest = clientUnderTestFactory.create(clientConfiguration);

			boolean result = clientUnderTest.ping();

			assertThat(result).isTrue();
			verify(headRequestedFor(urlEqualTo("/")));
			hoverfly.verify(service(serviceHost).head("/"), atLeast(1));
		});
	}

	@ParameterizedTest(name = "{0}") // DATAES-801, DATAES-588
	@MethodSource("clientUnderTestFactorySource")
	@DisplayName("should configure client and set all required headers")
	void shouldConfigureClientAndSetAllRequiredHeaders(ClientUnderTestFactory clientUnderTestFactory) {

		wireMockServer(server -> {

			HttpHeaders defaultHeaders = new HttpHeaders();
			defaultHeaders.addAll("def1", Arrays.asList("def1-1", "def1-2"));
			defaultHeaders.add("def2", "def2-1");

			AtomicInteger supplierCount = new AtomicInteger(1);
			AtomicInteger httpClientConfigurerCount = new AtomicInteger(0);
			AtomicInteger restClientConfigurerCount = new AtomicInteger(0);

			ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();
			configurationBuilder //
					.connectedTo("localhost:" + server.port()) //
					.withBasicAuth("user", "password") //
					.withDefaultHeaders(defaultHeaders) //
					.withHeaders(() -> {
						HttpHeaders httpHeaders = new HttpHeaders();
						httpHeaders.add("supplied", "val0");
						httpHeaders.add("supplied", "val" + supplierCount.getAndIncrement());
						return httpHeaders;
					});

			if (clientUnderTestFactory instanceof ELCUnderTestFactory) {
				configurationBuilder.withClientConfigurer(
						ElasticsearchClients.ElasticsearchHttpClientConfigurationCallback.from(httpClientBuilder -> {
							httpClientConfigurerCount.incrementAndGet();
							return httpClientBuilder;
						}));
				configurationBuilder.withClientConfigurer(
						ElasticsearchClients.ElasticsearchRestClientConfigurationCallback.from(restClientBuilder -> {
							restClientConfigurerCount.incrementAndGet();
							return restClientBuilder;
						}));
			} else if (clientUnderTestFactory instanceof ReactiveELCUnderTestFactory) {
				configurationBuilder
						.withClientConfigurer(ElasticsearchClients.ElasticsearchHttpClientConfigurationCallback.from(webClient -> {
							httpClientConfigurerCount.incrementAndGet();
							return webClient;
						}));
				configurationBuilder.withClientConfigurer(
						ElasticsearchClients.ElasticsearchRestClientConfigurationCallback.from(restClientBuilder -> {
							restClientConfigurerCount.incrementAndGet();
							return restClientBuilder;
						}));
			}

			ClientConfiguration clientConfiguration = configurationBuilder.build();

			ClientUnderTest clientUnderTest = clientUnderTestFactory.create(clientConfiguration);

			// do several calls to check that the headerSupplier provided values are set
			int startValue = clientUnderTest.usesInitialRequest() ? 2 : 1;
			for (int i = startValue; i <= startValue + 2; i++) {
				clientUnderTest.ping();

				verify(headRequestedFor(urlEqualTo("/")) //
						.withHeader("Authorization", new AnythingPattern()) //
						.withHeader("def1", new EqualToPattern("def1-1")) //
						.withHeader("def1", new EqualToPattern("def1-2")) //
						.withHeader("def2", new EqualToPattern("def2-1")) //
						.withHeader("supplied", new EqualToPattern("val0")) //
						// on the first call Elasticsearch does the version check and thus already increments the counter
						.withHeader("supplied", new EqualToPattern("val" + (i))) //
				);
			}

			assertThat(httpClientConfigurerCount).hasValue(1);
			assertThat(restClientConfigurerCount).hasValue(clientUnderTestFactory.getExpectedRestClientConfigCalls());
		});
	}

	@ParameterizedTest // #2088
	@MethodSource("clientUnderTestFactorySource")
	@DisplayName("should set compatibility headers")
	void shouldSetCompatibilityHeaders(ClientUnderTestFactory clientUnderTestFactory) {

		wireMockServer(server -> {

			String urlPattern = "^/index/_doc/42(\\?.*)?$";
			stubFor(put(urlMatching(urlPattern)) //
					.willReturn(jsonResponse("""
							{
							  "_id": "42",
							  "_index": "test",
							  "_primary_term": 1,
							  "_seq_no": 0,
							  "_shards": {
							    "failed": 0,
							    "successful": 1,
							    "total": 2
							  },
							  "_type": "_doc",
							  "_version": 1,
							  "result": "created"
							}
							""" //
							, 201) //
							.withHeader("Content-Type", "application/vnd.elasticsearch+json;compatible-with=7") //
							.withHeader("X-Elastic-Product", "Elasticsearch")));

			ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();
			configurationBuilder //
					.connectedTo("localhost:" + server.port()) //
					.withHeaders(() -> {
						HttpHeaders defaultCompatibilityHeaders = new HttpHeaders();
						defaultCompatibilityHeaders.add("Accept", "application/vnd.elasticsearch+json;compatible-with=7");
						defaultCompatibilityHeaders.add("Content-Type", "application/vnd.elasticsearch+json;compatible-with=7");
						return defaultCompatibilityHeaders;
					});

			ClientConfiguration clientConfiguration = configurationBuilder.build();
			ClientUnderTest clientUnderTest = clientUnderTestFactory.create(clientConfiguration);

			class Foo {
				public final String id;

				Foo(String id) {
					this.id = id;
				}
			}

            clientUnderTest.index(new Foo("42"));

			verify(putRequestedFor(urlMatching(urlPattern)) //
					.withHeader("Accept", new EqualToPattern("application/vnd.elasticsearch+json;compatible-with=7")) //
					.withHeader("Content-Type", new EqualToPattern("application/vnd.elasticsearch+json;compatible-with=7")) //
			);
		});
	}

	private StubMapping stubForElasticsearchVersionCheck() {
		return stubFor(get(urlEqualTo("/")) //
				.willReturn(okJson("""
						{
						    "cluster_name": "docker-cluster",
						    "cluster_uuid": "nKasrfHjRo6ge0eBmMUuAQ",
						    "name": "c1a6e517d001",
						    "tagline": "You Know, for Search",
						    "version": {
						        "build_date": "2021-08-26T09:01:05.390870785Z",
						        "build_flavor": "default",
						        "build_hash": "66b55ebfa59c92c15db3f69a335d500018b3331e",
						        "build_snapshot": false,
						        "build_type": "docker",
						        "lucene_version": "8.9.0",
						        "minimum_index_compatibility_version": "6.0.0-beta1",
						        "minimum_wire_compatibility_version": "6.8.0",
						        "number": "7.14.1"
						    }
						}""") //
						.withHeader("Content-Type", "application/json; charset=UTF-8") //
						.withHeader("X-Elastic-Product", "Elasticsearch")));
	}

	private StubMapping stubForHead() {
		return stubFor(head(urlEqualTo("/")) //
				.willReturn(ok() //
						.withHeader("X-Elastic-Product", "Elasticsearch")));
	}

	/**
	 * Consumer extension that catches checked exceptions and wraps them in a RuntimeException.
	 */
	@FunctionalInterface
	interface WiremockConsumer extends Consumer<WireMockServer> {
		@Override
		default void accept(WireMockServer wiremockConsumer) {
			try {
				acceptThrows(wiremockConsumer);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		void acceptThrows(WireMockServer wiremockConsumer) throws Exception;
	}

	/**
	 * starts a Wiremock server and calls consumer with the server as argument. Stops the server after consumer execution.
	 * Before the consumer ids called the {@link #stubForHead()} and {@link #stubForElasticsearchVersionCheck()} are
	 * registered.
	 *
	 * @param consumer the consumer
	 */
	private void wireMockServer(WiremockConsumer consumer) {
		WireMockServer wireMockServer = new WireMockServer(options() //
				.dynamicPort() //
				.usingFilesUnderDirectory("src/test/resources/wiremock-mappings")); // needed, otherwise Wiremock goes to
		// test/resources/mappings
		try {
			wireMockServer.start();
			WireMock.configureFor(wireMockServer.port());
			stubForHead();
			stubForElasticsearchVersionCheck();

			consumer.accept(wireMockServer);
		} finally {
			wireMockServer.shutdown();
		}
	}

	/**
	 * The client to be tested. Abstraction to be able to test reactive and non-reactive clients.
	 */
	interface ClientUnderTest {
		/**
		 * Pings the configured server. Must use a HEAD request to "/".
		 *
		 * @return true if successful
		 */
		boolean ping() throws Exception;

		/**
		 * @return true if the client send an initial request for discovery, version check or similar.
		 */
		boolean usesInitialRequest();

		/**
		 * save an entity using an index request
		 */
		<T> void index(T entity) throws IOException;
	}

	/**
	 * base class to create {@link ClientUnderTest} implementations.
	 */
	static abstract class ClientUnderTestFactory {
		abstract ClientUnderTest create(ClientConfiguration clientConfiguration);

		@Override
		public String toString() {
			return getDisplayName();
		}

		protected abstract String getDisplayName();

		protected Integer getExpectedRestClientConfigCalls() {
			return 0;
		}
	}

	/**
	 * {@link ClientUnderTestFactory} implementation for the {@link co.elastic.clients.elasticsearch.ElasticsearchClient}.
	 */
	static class ELCUnderTestFactory extends ClientUnderTestFactory {

		@Override
		protected String getDisplayName() {
			return "ElasticsearchClient";
		}

		@Override
		protected Integer getExpectedRestClientConfigCalls() {
			return 1;
		}

		@Override
		ClientUnderTest create(ClientConfiguration clientConfiguration) {

			ElasticsearchClient client = ElasticsearchClients.createImperative(clientConfiguration);
			return new ClientUnderTest() {
				@Override
				public boolean ping() throws Exception {
					return client.ping().value();
				}

				@Override
				public boolean usesInitialRequest() {
					return false;
				}

				@Override
				public <T> void index(T entity) throws IOException {
					client.index(ir -> ir.index("index").id("42").document(entity));
				}
			};
		}
	}

	/**
	 * {@link ClientUnderTestFactory} implementation for the {@link ReactiveElasticsearchClient}.
	 */
	static class ReactiveELCUnderTestFactory extends ClientUnderTestFactory {

		@Override
		protected String getDisplayName() {
			return "ReactiveElasticsearchClient (ELC based)";
		}

		@Override
		protected Integer getExpectedRestClientConfigCalls() {
			return 1;
		}

		@Override
		ClientUnderTest create(ClientConfiguration clientConfiguration) {

			ReactiveElasticsearchClient client = ElasticsearchClients.createReactive(clientConfiguration);
			return new ClientUnderTest() {
				@Override
				public boolean ping() throws Exception {
					return Boolean.TRUE.equals(client.ping().map(BooleanResponse::value).block());
				}

				@Override
				public boolean usesInitialRequest() {
					return false;
				}

				@Override
				public <T> void index(T entity) throws IOException {
					client.index(ir -> ir.index("index").id("42").document(entity)).block();
				}
			};
		}
	}

	/**
	 * Provides the factories to use in the parameterized tests
	 *
	 * @return stream of factories
	 */
	static Stream<ClientUnderTestFactory> clientUnderTestFactorySource() {
		return Stream.of( //
				new ELCUnderTestFactory(), //
				new ReactiveELCUnderTestFactory());
	}
}
