/*
 * Copyright 2020-2024 the original author or authors.
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

import static org.springframework.util.StringUtils.*;

import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.elasticsearch.ResourceFailureException;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.util.Assert;

/**
 * Utility to reactively read {@link org.springframework.core.io.Resource}s.
 *
 * @author Peter-Josef Meisch
 * @since 4.1
 */
public abstract class ReactiveResourceUtil {

	private static final Log LOGGER = LogFactory.getLog(ReactiveResourceUtil.class);

	private static final int BUFFER_SIZE = 8_192;

	/**
	 * Read a {@link ClassPathResource} into a {@link reactor.core.publisher.Mono<String>}.
	 *
	 * @param url the resource to read
	 * @return a {@link reactor.core.publisher.Mono} emitting the resources content or an empty Mono on error
	 */
	public static Mono<String> readFileFromClasspath(String url) {

		Assert.notNull(url, "url must not be null");

		return DataBufferUtils
				.join(DataBufferUtils.read(new ClassPathResource(url), new DefaultDataBufferFactory(), BUFFER_SIZE))
				.<String> handle((it, sink) -> {

					try (InputStream is = it.asInputStream();
							InputStreamReader in = new InputStreamReader(is, Charset.defaultCharset());
							BufferedReader br = new BufferedReader(in)) {

						StringBuilder sb = new StringBuilder();

						String line;
						while ((line = br.readLine()) != null) {
							sb.append(line).append('\n');
						}

						sink.next(sb.toString());
						sink.complete();
					} catch (Exception e) {
						sink.complete();
					} finally {
						DataBufferUtils.release(it);
					}
				}).onErrorResume(
						throwable -> Mono.error(new ResourceFailureException("Could not load resource from " + url, throwable)));
	}

	/**
	 * loads a Document initialized with data from a given resource path.
	 *
	 * @param path the path to load data from
	 * @param annotation the annotation that had the resource path defined
	 * @return the parsed document
	 * @since 4.4
	 */
	public static Mono<Document> loadDocument(String path, String annotation) {

		if (hasText(path)) {
			return readFileFromClasspath(path).flatMap(s -> {
				if (hasText(s)) {
					return Mono.just(Document.parse(s));
				} else {
					return Mono.just(Document.create());
				}
			});
		} else {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("path in %s has to be defined. Using default instead.", annotation));
			}
		}

		return Mono.just(Document.create());
	}

	// Utility constructor
	private ReactiveResourceUtil() {}
}
