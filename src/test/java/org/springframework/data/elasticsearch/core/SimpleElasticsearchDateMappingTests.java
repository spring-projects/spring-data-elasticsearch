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
package org.springframework.data.elasticsearch.core;

import java.beans.IntrospectionException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.elasticsearch.entities.SampleDateMappingEntity;

/**
 * @author Jakub Vavrik
 * @author Mohsin Husen
 * @author Don Wellington
 */
public class SimpleElasticsearchDateMappingTests {

	private static final String EXPECTED_MAPPING = "{\"mapping\":{\"properties\":{\"message\":{\"store\":true," +
			"\"type\":\"text\",\"index\":false,\"analyzer\":\"standard\"},\"customFormatDate\":{\"store\":false,\"type\":\"date\",\"format\":\"dd.MM.yyyy hh:mm\"}," +
			"\"defaultFormatDate\":{\"store\":false,\"type\":\"date\"},\"basicFormatDate\":{\"store\":false,\"" +
			"type\":\"date\",\"format\":\"basic_date\"}}}}";

	@Test
	public void testCorrectDateMappings() throws NoSuchFieldException, IntrospectionException, IOException {
		XContentBuilder xContentBuilder = MappingBuilder.buildMapping(SampleDateMappingEntity.class, "mapping", "id", null);
		xContentBuilder.close();
		ByteArrayOutputStream bos = (ByteArrayOutputStream) xContentBuilder.getOutputStream();
		String result = bos.toString();
		Assert.assertEquals(EXPECTED_MAPPING, result);
	}
}
