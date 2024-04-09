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

import co.elastic.clients.json.JsonpMapper;
import jakarta.json.stream.JsonGenerator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
final class JsonUtils {

	private static final Log LOGGER = LogFactory.getLog(JsonUtils.class);

	private JsonUtils() {}

	public static String toJson(Object object, JsonpMapper mapper) {

		// noinspection SpellCheckingInspection
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator generator = mapper.jsonProvider().createGenerator(baos);
		mapper.serialize(object, generator);
		generator.close();
		String json = "{}";
		json = baos.toString(StandardCharsets.UTF_8);
		return json;
	}

	@Nullable
	public static String queryToJson(@Nullable co.elastic.clients.elasticsearch._types.query_dsl.Query query,
			JsonpMapper mapper) {

		if (query == null) {
			return null;
		}

		var baos = new ByteArrayOutputStream();
		var generator = mapper.jsonProvider().createGenerator(baos);
		query.serialize(generator, mapper);
		generator.close();
		return baos.toString(StandardCharsets.UTF_8);
	}

}
