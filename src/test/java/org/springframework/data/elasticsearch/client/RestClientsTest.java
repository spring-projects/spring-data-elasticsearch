/*
 * Copyright 2019-2021 the original author or authors.
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

import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import io.specto.hoverfly.junit5.api.HoverflyCapture;
import io.specto.hoverfly.junit5.api.HoverflyConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.http.HttpHeaders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

/**
 * We need hoverfly for testing the reactive code to use a proxy. Wiremock cannot intercept the proxy calls as WebClient
 * uses HTTP CONNECT on proxy requests which wiremock does not support.
 *
 * @author Peter-Josef Meisch
 */
@HoverflyCapture(path = "target/hoverfly", config = @HoverflyConfig(proxyLocalHost = true, plainHttpTunneling = true))
@ExtendWith(HoverflyExtension.class)
public class RestClientsTest {

	@ParameterizedTest // DATAES-700
	@MethodSource("clientUnderTestFactorySource")
	@DisplayName("should use configured proxy")
	void shouldUseConfiguredProxy(ClientUnderTestFactory clientUnderTestFactory, Hoverfly hoverfly) throws IOException {

		wireMockServer(server -> {

			// wiremock is the dummy server, hoverfly the proxy
			WireMock.configureFor(server.port());
			stubForElasticsearchVersionCheck();
			stubFor(head(urlEqualTo("/")).willReturn(aResponse() //
					.withHeader("Content-Type", "application/json; charset=UTF-8")));

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

	@ParameterizedTest // DATAES-801, DATAES-588
	@MethodSource("clientUnderTestFactorySource")
	@DisplayName("should configure client and set all required headers")
	void shouldConfigureClientAndSetAllRequiredHeaders(ClientUnderTestFactory clientUnderTestFactory) {
		wireMockServer(server -> {

			WireMock.configureFor(server.port());

			stubForElasticsearchVersionCheck();
			stubFor(head(urlEqualTo("/")).willReturn(aResponse() //
					.withHeader("Content-Type", "application/json; charset=UTF-8")));

			HttpHeaders defaultHeaders = new HttpHeaders();
			defaultHeaders.addAll("def1", Arrays.asList("def1-1", "def1-2"));
			defaultHeaders.add("def2", "def2-1");

			AtomicInteger supplierCount = new AtomicInteger(1);
			AtomicInteger clientConfigurerCount = new AtomicInteger(0);

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

			if (clientUnderTestFactory instanceof RestClientUnderTestFactory) {
				configurationBuilder
						.withClientConfigurer(RestClients.RestClientConfigurationCallback.from(httpClientBuilder -> {
							clientConfigurerCount.incrementAndGet();
							return httpClientBuilder;
						}));
			} else if (clientUnderTestFactory instanceof ReactiveElasticsearchClientUnderTestFactory) {
				configurationBuilder.withClientConfigurer(ReactiveRestClients.WebClientConfigurationCallback.from(webClient -> {
					clientConfigurerCount.incrementAndGet();
					return webClient;
				}));
			}

			ClientConfiguration clientConfiguration = configurationBuilder.build();

			ClientUnderTest clientUnderTest = clientUnderTestFactory.create(clientConfiguration);

			// do several calls to check that the headerSupplier provided values are set
			for (int i = 1; i <= 3; i++) {
				clientUnderTest.ping();

				verify(headRequestedFor(urlEqualTo("/")) //
						.withHeader("Authorization", new AnythingPattern()) //
						.withHeader("def1", new EqualToPattern("def1-1")) //
						.withHeader("def1", new EqualToPattern("def1-2")) //
						.withHeader("def2", new EqualToPattern("def2-1")) //
						.withHeader("supplied", new EqualToPattern("val0")) //
						// on the first call Elasticsearch does the version check and thus already increments the counter
						.withHeader("supplied", new EqualToPattern("val" + (i + 1))) //
				);
			}

			assertThat(clientConfigurerCount).hasValue(1);
		});
	}

	private StubMapping stubForElasticsearchVersionCheck() {
		return stubFor(get(urlEqualTo("/")) //
				.willReturn(okJson("{\n" + //
						"    \"cluster_name\": \"docker-cluster\",\n" + //
						"    \"cluster_uuid\": \"nKasrfHjRo6ge0eBmMUuAQ\",\n" + //
						"    \"name\": \"c1a6e517d001\",\n" + //
						"    \"tagline\": \"You Know, for Search\",\n" + //
						"    \"version\": {\n" + //
						"        \"build_date\": \"2021-08-26T09:01:05.390870785Z\",\n" + //
						"        \"build_flavor\": \"default\",\n" + //
						"        \"build_hash\": \"66b55ebfa59c92c15db3f69a335d500018b3331e\",\n" + //
						"        \"build_snapshot\": false,\n" + //
						"        \"build_type\": \"docker\",\n" + //
						"        \"lucene_version\": \"8.9.0\",\n" + //
						"        \"minimum_index_compatibility_version\": \"6.0.0-beta1\",\n" + //
						"        \"minimum_wire_compatibility_version\": \"6.8.0\",\n" + //
						"        \"number\": \"7.14.1\"\n" + //
						"    }\n" + //
						"}") //
								.withHeader("Content-Type", "application/json; charset=UTF-8") //
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
	}

	/**
	 * {@link ClientUnderTestFactory} implementation for the Standard {@link RestHighLevelClient}.
	 */
	static class RestClientUnderTestFactory extends ClientUnderTestFactory {

		@Override
		protected String getDisplayName() {
			return "RestHighLevelClient";
		}

		@Override
		ClientUnderTest create(ClientConfiguration clientConfiguration) {
			RestHighLevelClient client = RestClients.create(clientConfiguration).rest();
			return () -> client.ping(RequestOptions.DEFAULT);
		}

	}

	/**
	 * {@link ClientUnderTestFactory} implementation for the {@link ReactiveElasticsearchClient}.
	 */
	static class ReactiveElasticsearchClientUnderTestFactory extends ClientUnderTestFactory {

		@Override
		protected String getDisplayName() {
			return "ReactiveElasticsearchClient";
		}

		@Override
		ClientUnderTest create(ClientConfiguration clientConfiguration) {
			ReactiveElasticsearchClient client = ReactiveRestClients.create(clientConfiguration);
			return () -> client.ping().block();
		}
	}

	/**
	 * Provides the factories to use in the parameterized tests
	 *
	 * @return stream of factories
	 */
	static Stream<ClientUnderTestFactory> clientUnderTestFactorySource() {
		return Stream.of( //
				new RestClientUnderTestFactory(), //
				new ReactiveElasticsearchClientUnderTestFactory() //
		);
	}
}
