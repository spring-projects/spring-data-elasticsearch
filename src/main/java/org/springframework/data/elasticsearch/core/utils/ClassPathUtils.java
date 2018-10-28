/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.elasticsearch.core.utils;

import lombok.experimental.*;
import lombok.extern.slf4j.*;
import org.springframework.core.io.*;

import java.nio.file.*;

@Slf4j
@UtilityClass
public class ClassPathUtils {

	public static String readFileFromClasspath(String url) {
		try {
			ClassPathResource classPathResource = new ClassPathResource(url);
			return new String(Files.readAllBytes(Paths.get(classPathResource.getURI())));
		} catch (Exception e) {
			log.error(String.format("Failed to load file from url: %s: %s", url, e.getMessage()));
			return null;
		}
	}
}
