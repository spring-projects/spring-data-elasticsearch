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
package org.springframework.data.elasticsearch.core.convert;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions.Base64ToByteArrayConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions.ByteArrayToBase64Converter;

/**
 * @author Peter-Josef Meisch
 */
class ElasticsearchCustomConversionsUnitTests {

	private final byte[] bytes = new byte[] { 0x01, 0x02, 0x03, 0x04 };
	private final String base64 = "AQIDBA==";

	@Test
	void shouldConvertFromByteArrayToBase64() {
		assertThat(ByteArrayToBase64Converter.INSTANCE.convert(bytes)).isEqualTo(base64);
	}

	@Test
	void shouldConvertFromStringToBase64() {
		assertThat(Base64ToByteArrayConverter.INSTANCE.convert(base64)).isEqualTo(bytes);
	}
}
