/*
 * Copyright 2026-present the original author or authors.
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
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;

/**
 * @author Minju Kim
 * @author Shubham Singh
 */
class ElasticsearchClientsUnitTests {

	@Test // #3361
	@DisplayName("should create reactive clients from a ClientConfiguration with a Rest5Client")
	void shouldCreateReactiveClientsFromClientConfigurationWithRest5Client() throws IOException {

		var clientConfiguration = ClientConfiguration.localhost();
		TransportOptions transportOptions = new Rest5ClientOptions.Builder(
				co.elastic.clients.transport.rest5_client.low_level.RequestOptions.DEFAULT.toBuilder()).build();

		List<ReactiveElasticsearchClient> clients = List.of( //
				ElasticsearchClients.createReactive(clientConfiguration), //
				ElasticsearchClients.createReactive(clientConfiguration, transportOptions), //
				ElasticsearchClients.createReactive(clientConfiguration, transportOptions, new Jackson3JsonpMapper()));

		for (ReactiveElasticsearchClient client : clients) {
			try {
				assertThat(client._transport()).isInstanceOf(Rest5ClientTransport.class);
			} finally {
				client._transport().close();
			}
		}
	}

	@Test // #3361
	@DisplayName("should create clients from a ClientConfiguration when the old RestClient is not on the classpath")
	@SuppressWarnings("unchecked")
	void shouldCreateClientsWhenOldRestClientIsNotOnTheClasspath() throws Exception {

		try (var classLoader = new HidingClassLoader(name -> name.startsWith("org.elasticsearch.client."))) {

			var createClients = (Supplier<List<String>>) classLoader
					.loadClass(CreateClientsFromConfiguration.class.getName()).getDeclaredConstructor().newInstance();

			assertThat(createClients.get()).hasSize(2).containsOnly(Rest5ClientTransport.class.getName());
		}
	}

	@Test // #3351
	@DisplayName("should create clients without an exception when Jackson 2 is not on the classpath")
	@SuppressWarnings("unchecked")
	void shouldCreateClientsWithoutExceptionWhenJackson2IsNotOnTheClasspath() throws Exception {

		try (var classLoader = new HidingClassLoader(
				name -> name.startsWith("com.fasterxml.jackson.core.") || name.startsWith("com.fasterxml.jackson.databind."))) {

			var createClients = (Supplier<List<String>>) classLoader.loadClass(CreateClients.class.getName())
					.getDeclaredConstructor().newInstance();

			assertThat(createClients.get()).hasSize(4).containsOnly(Jackson3JsonpMapper.class.getName());
		}
	}

	/**
	 * Loaded by the {@link Jackson2HidingClassLoader}, creates the clients and returns the class names of their
	 * JsonpMappers.
	 */
	public static class CreateClients implements Supplier<List<String>> {

		@Override
		public List<String> get() {

			var clientConfiguration = ClientConfiguration.localhost();
			Rest5Client rest5Client = Rest5Clients.getRest5Client(clientConfiguration);

			List<ElasticsearchClient> imperativeClients = List.of( //
					ElasticsearchClients.createImperative(clientConfiguration), //
					ElasticsearchClients.createImperative(rest5Client));
			List<ReactiveElasticsearchClient> reactiveClients = List.of( //
					ElasticsearchClients.createReactive(clientConfiguration), //
					ElasticsearchClients.createReactive(rest5Client));

			List<String> mapperClassNames = new ArrayList<>();
			try {
				for (ElasticsearchClient client : imperativeClients) {
					mapperClassNames.add(client._jsonpMapper().getClass().getName());
					client._transport().close();
				}
				for (ReactiveElasticsearchClient client : reactiveClients) {
					mapperClassNames.add(client._jsonpMapper().getClass().getName());
					client._transport().close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return mapperClassNames;
		}
	}

	/**
	 * Loaded by the {@link HidingClassLoader}, creates the clients from a {@link ClientConfiguration} and returns the
	 * class names of their transports.
	 */
	public static class CreateClientsFromConfiguration implements Supplier<List<String>> {

		@Override
		public List<String> get() {

			var clientConfiguration = ClientConfiguration.localhost();
			ElasticsearchClient imperativeClient = ElasticsearchClients.createImperative(clientConfiguration);
			ReactiveElasticsearchClient reactiveClient = ElasticsearchClients.createReactive(clientConfiguration);

			try {
				List<String> transportClassNames = List.of(imperativeClient._transport().getClass().getName(),
						reactiveClient._transport().getClass().getName());
				imperativeClient._transport().close();
				reactiveClient._transport().close();
				return transportClassNames;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Class loader with the test classpath that does not find the classes matching the given predicate, like an
	 * application that excludes the corresponding dependencies.
	 */
	static class HidingClassLoader extends URLClassLoader {

		private final Predicate<String> hidden;

		HidingClassLoader(Predicate<String> hidden) throws MalformedURLException {
			super(classpath(), ClassLoader.getPlatformClassLoader());
			this.hidden = hidden;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

			if (hidden.test(name)) {
				throw new ClassNotFoundException(name);
			}
			return super.loadClass(name, resolve);
		}

		private static URL[] classpath() throws MalformedURLException {

			String[] entries = System.getProperty("java.class.path").split(File.pathSeparator);
			URL[] urls = new URL[entries.length];
			for (int i = 0; i < entries.length; i++) {
				urls[i] = new File(entries[i]).toURI().toURL();
			}
			return urls;
		}
	}
}
