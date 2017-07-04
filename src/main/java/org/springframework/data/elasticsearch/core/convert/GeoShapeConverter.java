/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.elasticsearch.core.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.elasticsearch.core.geo.GeoShape;
import org.springframework.data.elasticsearch.core.geo.GeoShapeModule;

/**
 * GeoShapeConverter
 *
 * @author Lukas Vorisek
 */

@SuppressWarnings("rawtypes")
public final class GeoShapeConverter implements Converter<GeoShape, String> {
	private static GeoShapeConverter instance;

	private GeoShapeConverter() {
		// nothing
	}

	public static GeoShapeConverter getInstance() {
		if(instance == null) {
			synchronized (GeoShapeConverter.class) {
				if(instance == null) {
					GeoShapeConverter.instance = new GeoShapeConverter();
				}
			}
		}

		return GeoShapeConverter.instance;
	}



	@Override
	public String convert(GeoShape source) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new GeoShapeModule());

		try {
			return objectMapper.writeValueAsString(source);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
