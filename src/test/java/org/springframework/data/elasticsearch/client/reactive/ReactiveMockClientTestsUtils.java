/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.client.reactive;

import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.reactive.ReactiveMockClientTestsUtils.MockWebClientProvider.Send;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * @author Christoph Strobl
 * @author Huw Ayling-Miller
 * @author Henrique Amaral
 * @author Peter-Josef Meisch
 */
public class ReactiveMockClientTestsUtils {

	private final static Map<String, InetSocketAddress> ADDRESS_CACHE = new ConcurrentHashMap<>();

	public static MockDelegatingElasticsearchHostProvider<SingleNodeHostProvider> single(String host) {
		return provider(host);
	}

	public static MockDelegatingElasticsearchHostProvider<MultiNodeHostProvider> multi(String... hosts) {
		return provider(hosts);
	}

	public static <T extends HostProvider<T>> MockDelegatingElasticsearchHostProvider<T> provider(String... hosts) {

		ErrorCollector errorCollector = new ErrorCollector();
		MockWebClientProvider clientProvider = new MockWebClientProvider(errorCollector);
		T delegate;

		if (hosts.length == 1) {
			// noinspection unchecked
			delegate = (T) new SingleNodeHostProvider(clientProvider, HttpHeaders::new, getInetSocketAddress(hosts[0])) {};
		} else {
			// noinspection unchecked
			delegate = (T) new MultiNodeHostProvider(clientProvider, HttpHeaders::new, Arrays.stream(hosts)
					.map(ReactiveMockClientTestsUtils::getInetSocketAddress).toArray(InetSocketAddress[]::new)) {};
		}

		return new MockDelegatingElasticsearchHostProvider<>(HttpHeaders.EMPTY, clientProvider, errorCollector, delegate,
				null);
	}

	private static InetSocketAddress getInetSocketAddress(String hostAndPort) {
		return ADDRESS_CACHE.computeIfAbsent(hostAndPort, ElasticsearchHost::parse);
	}

	public static class ErrorCollector implements Consumer<Throwable> {

		List<Throwable> errors = new CopyOnWriteArrayList<>();

		@Override
		public void accept(Throwable throwable) {
			errors.add(throwable);
		}

		List<Throwable> captured() {
			return Collections.unmodifiableList(errors);
		}
	}

	public static class MockDelegatingElasticsearchHostProvider<T extends HostProvider<T>> implements HostProvider<T> {

		private final HttpHeaders httpHeaders;
		private final T delegate;
		private final MockWebClientProvider clientProvider;
		private final ErrorCollector errorCollector;
		private @Nullable final String activeDefaultHost;

		public MockDelegatingElasticsearchHostProvider(HttpHeaders httpHeaders, MockWebClientProvider clientProvider,
				ErrorCollector errorCollector, T delegate, @Nullable String activeDefaultHost) {

			this.httpHeaders = httpHeaders;
			this.errorCollector = errorCollector;
			this.clientProvider = clientProvider;
			this.delegate = delegate;
			this.activeDefaultHost = activeDefaultHost;
		}

		@Override
		public Mono<InetSocketAddress> lookupActiveHost() {
			return delegate.lookupActiveHost();
		}

		@Override
		public Mono<InetSocketAddress> lookupActiveHost(Verification verification) {

			if (StringUtils.hasText(activeDefaultHost)) {
				return Mono.just(getInetSocketAddress(activeDefaultHost));
			}

			return delegate.lookupActiveHost(verification);
		}

		@Override
		public Mono<WebClient> getActive() {
			return delegate.getActive();
		}

		@Override
		public Mono<WebClient> getActive(Verification verification) {
			return delegate.getActive(verification);
		}

		@Override
		public WebClient createWebClient(InetSocketAddress endpoint) {
			return delegate.createWebClient(endpoint);
		}

		@Override
		public Mono<ClusterInformation> clusterInfo() {

			if (StringUtils.hasText(activeDefaultHost)) {
				return Mono.just(new ClusterInformation(
						Collections.singleton(ElasticsearchHost.online(getInetSocketAddress(activeDefaultHost)))));
			}

			return delegate.clusterInfo();
		}

		public Send when(String host) {
			return clientProvider.when(host);
		}

		public WebClient client(String host) {
			return clientProvider.when(host).client();
		}

		public List<Throwable> errors() {
			return errorCollector.captured();
		}

		public T getDelegate() {
			return delegate;
		}

		public MockDelegatingElasticsearchHostProvider<T> withActiveDefaultHost(String host) {
			return new MockDelegatingElasticsearchHostProvider<>(httpHeaders, clientProvider, errorCollector, delegate, host);
		}
	}

	public static class MockWebClientProvider implements WebClientProvider {

		private final Consumer<Throwable> errorListener;

		private final Map<InetSocketAddress, WebClient> clientMap;
		private final Map<InetSocketAddress, RequestHeadersUriSpec> headersUriSpecMap;
		private final Map<InetSocketAddress, RequestBodyUriSpec> bodyUriSpecMap;
		private final Map<InetSocketAddress, ClientResponse> responseMap;

		public MockWebClientProvider(Consumer<Throwable> errorListener) {

			this.errorListener = errorListener;
			this.clientMap = new ConcurrentHashMap<>();
			this.headersUriSpecMap = new LinkedHashMap<>();
			this.bodyUriSpecMap = new LinkedHashMap<>();
			this.responseMap = new LinkedHashMap<>();
		}

		public WebClient get(String host) {
			return get(getInetSocketAddress(host));
		}

		@Override
		public WebClient get(InetSocketAddress endpoint) {

			return clientMap.computeIfAbsent(endpoint, key -> {

				WebClient webClient = mock(WebClient.class);

				RequestHeadersUriSpec headersUriSpec = mock(RequestHeadersUriSpec.class);
				Mockito.when(headersUriSpec.uri(any(String.class))).thenReturn(headersUriSpec);
				Mockito.when(headersUriSpec.uri(any(), any(Map.class))).thenReturn(headersUriSpec);
				Mockito.when(headersUriSpec.headers(any(Consumer.class))).thenReturn(headersUriSpec);
				Mockito.when(headersUriSpec.attribute(anyString(), anyString())).thenReturn(headersUriSpec);
				Mockito.when(headersUriSpec.uri(any(Function.class))).thenReturn(headersUriSpec);
				headersUriSpecMap.putIfAbsent(key, headersUriSpec);

				ClientResponse response = mock(ClientResponse.class);
				Mockito.when(response.statusCode()).thenReturn(HttpStatus.ACCEPTED);
				Mockito.when(response.releaseBody()).thenReturn(Mono.empty());
				Mockito.when(headersUriSpec.exchangeToMono(any())).thenAnswer(invocation -> {
					final Function<ClientResponse, ? extends Mono<?>> responseHandler = invocation.getArgument(0);

					if (responseHandler != null) {
						return responseHandler.apply(response);
					}
					return Mono.empty();
				});
				responseMap.putIfAbsent(key, response);

				RequestBodyUriSpec bodySpy = spy(WebClient.create().method(HttpMethod.POST));
				Mockito.when(bodySpy.body(any())).thenReturn(headersUriSpec);
				Mockito.when(bodySpy.exchangeToMono(any())).thenAnswer(invocation -> {
					final Function<ClientResponse, ? extends Mono<?>> responseHandler = invocation.getArgument(0);

					if (responseHandler != null) {
						return responseHandler.apply(response);
					}
					return Mono.empty();
				});
				bodyUriSpecMap.putIfAbsent(key, bodySpy);

				Mockito.when(webClient.get()).thenReturn(headersUriSpec);
				Mockito.when(webClient.head()).thenReturn(headersUriSpec);
				Mockito.when(webClient.method(any())).thenReturn(bodySpy);

				return webClient;
			});
		}

		@Override
		public HttpHeaders getDefaultHeaders() {
			return HttpHeaders.EMPTY;
		}

		@Override
		public WebClientProvider withDefaultHeaders(HttpHeaders headers) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Consumer<Throwable> getErrorListener() {
			return errorListener;
		}

		@Override
		public WebClientProvider withErrorListener(Consumer<Throwable> errorListener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPathPrefix() {
			return null;
		}

		@Override
		public WebClientProvider withPathPrefix(String pathPrefix) {
			throw new UnsupportedOperationException();
		}

		@Override
		public WebClientProvider withWebClientConfigurer(Function<WebClient, WebClient> webClientConfigurer) {
			throw new UnsupportedOperationException("not implemented");
		}

		public Send when(String host) {
			InetSocketAddress inetSocketAddress = getInetSocketAddress(host);
			return new CallbackImpl(get(host), headersUriSpecMap.get(inetSocketAddress),
					bodyUriSpecMap.get(inetSocketAddress), responseMap.get(inetSocketAddress));
		}

		public interface Client {
			WebClient client();
		}

		@SuppressWarnings("UnusedReturnValue")
		public interface Send extends Receive, Client {

			Receive get(Consumer<RequestHeadersUriSpec<?>> headerSpec);

			Receive exchange(Consumer<RequestBodyUriSpec> bodySpec);

			default URI captureUri() {

				Set<URI> capturingSet = new LinkedHashSet<>();

				exchange(requestBodyUriSpec -> {

					// noinspection unchecked
					ArgumentCaptor<Function<UriBuilder, URI>> fkt = ArgumentCaptor.forClass(Function.class);
					verify(requestBodyUriSpec).uri(fkt.capture());

					capturingSet.add(fkt.getValue().apply(new DefaultUriBuilderFactory().builder()));
				});

				return capturingSet.iterator().next();
			}

			default Receive receiveJsonFromFile(String file) {

				return receive(Receive::json) //
						.body(Receive.fromPath(file));
			}

			default Receive receiveInfo() {

				return receiveJsonFromFile("info") //
						.receive(Receive::ok);

			}

			default Receive receiveIndexCreated() {

				return receiveJsonFromFile("index-ok-created") //
						.receive(Receive::ok);
			}

			default Receive receiveIndexUpdated() {

				return receiveJsonFromFile("index-ok-updated") //
						.receive(Receive::ok);
			}

			default Receive receiveSearchOk() {

				return receiveJsonFromFile("search-ok-no-hits") //
						.receive(Receive::ok);
			}

			default Receive receiveGetByIdNotFound() {

				return receiveJsonFromFile("get-by-id-no-hit") //
						.receive(
								response -> Mockito.when(response.statusCode()).thenReturn(HttpStatus.ACCEPTED, HttpStatus.NOT_FOUND));
			}

			default Receive receiveGetById() {

				return receiveJsonFromFile("get-by-id-ok") //
						.receive(Receive::ok);
			}

			default Receive receiveUpdateOk() {

				return receiveJsonFromFile("update-ok-updated") //
						.receive(Receive::ok);
			}

			default Receive receiveDeleteOk() {

				return receiveJsonFromFile("update-ok-deleted") //
						.receive(Receive::ok);
			}

			default Receive updateFail() {

				return receiveJsonFromFile("update-error-not-found") //
						.receive(
								response -> Mockito.when(response.statusCode()).thenReturn(HttpStatus.ACCEPTED, HttpStatus.NOT_FOUND));
			}

			default Receive receiveBulkOk() {

				return receiveJsonFromFile("bulk-ok") //
						.receive(Receive::ok);
			}

		}

		public interface Receive {

			Receive receive(Consumer<ClientResponse> response);

			default Receive body(String json) {
				return body(() -> json.getBytes(StandardCharsets.UTF_8));
			}

			default Receive body(Supplier<byte[]> json) {
				return body(json.get());
			}

			default Receive body(Resource resource) {

				return body(() -> {
					try {
						return StreamUtils.copyToByteArray(resource.getInputStream());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}

			default Receive body(byte[] bytes) {
				return receive(response -> Mockito.when(response.body(any())).thenReturn(Mono.just(bytes)));
			}

			static void ok(ClientResponse response) {
				Mockito.when(response.statusCode()).thenReturn(HttpStatus.ACCEPTED);
			}

			static void error(ClientResponse response) {
				Mockito.when(response.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			static void notFound(ClientResponse response) {
				Mockito.when(response.statusCode()).thenReturn(HttpStatus.NOT_FOUND);
			}

			static void json(ClientResponse response) {

				ClientResponse.Headers headers = Mockito.mock(ClientResponse.Headers.class);
				Mockito.when(headers.contentType()).thenReturn(Optional.of(MediaType.APPLICATION_JSON));

				Mockito.when(response.headers()).thenReturn(headers);
			}

			static Resource fromPath(String filename) {
				return new ClassPathResource("/org/springframework/data/elasticsearch/client/" + filename + ".json");
			}
		}

		static class CallbackImpl implements Send, Receive {

			WebClient client;
			RequestHeadersUriSpec<?> headersUriSpec;
			RequestBodyUriSpec bodyUriSpec;
			ClientResponse responseDelegate;

			public CallbackImpl(WebClient client, RequestHeadersUriSpec<?> headersUriSpec, RequestBodyUriSpec bodyUriSpec,
					ClientResponse responseDelegate) {

				this.client = client;
				this.headersUriSpec = headersUriSpec;
				this.bodyUriSpec = bodyUriSpec;
				this.responseDelegate = responseDelegate;
			}

			@Override
			public Receive get(Consumer<RequestHeadersUriSpec<?>> uriSpec) {

				uriSpec.accept(headersUriSpec);
				return this;
			}

			@Override
			public Receive exchange(Consumer<RequestBodyUriSpec> bodySpec) {

				bodySpec.accept(this.bodyUriSpec);
				return this;
			}

			@Override
			public Receive receive(Consumer<ClientResponse> response) {

				response.accept(responseDelegate);
				return this;
			}

			@Override
			public WebClient client() {
				return client;
			}

		}
	}
}
