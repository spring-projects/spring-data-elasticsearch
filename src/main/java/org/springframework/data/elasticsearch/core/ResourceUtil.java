/*
 * Copyright 2019 the original author or authors.
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

import java.io.InputStream;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * Utility to read {@link org.springframework.core.io.Resource}s.
 *
 * @author Mark Paluch
 * @since 3.2
 */
abstract class ResourceUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtil.class);

	/**
	 * Read a {@link ClassPathResource} into a {@link String}.
	 *
	 * @param url
	 * @return
	 */
	public static String readFileFromClasspath(String url) {

		ClassPathResource classPathResource = new ClassPathResource(url);
		try (InputStream is = classPathResource.getInputStream()) {
			return StreamUtils.copyToString(is, Charset.defaultCharset());
		} catch (Exception e) {
			LOGGER.debug(String.format("Failed to load file from url: %s: %s", url, e.getMessage()));
			return null;
		}
	}

	// Utility constructor
	private ResourceUtil() {}
}
