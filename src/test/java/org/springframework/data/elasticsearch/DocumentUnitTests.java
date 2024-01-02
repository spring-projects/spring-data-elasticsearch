/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.document.Document;

/**
 * Unit tests for {@link Document}.
 *
 * @author Mark Paluch
 */
public class DocumentUnitTests {

	@Test // DATAES-628
	public void shouldCreateNewDocument() {

		Document document = Document.create().append("key", "value");

		assertThat(document).containsEntry("key", "value");
	}

	@Test // DATAES-628
	public void shouldCreateNewDocumentFromMap() {

		Document document = Document.from(Collections.singletonMap("key", "value"));

		assertThat(document).containsEntry("key", "value");
	}

	@Test // DATAES-628
	public void shouldRenderDocumentToJson() {

		Document document = Document.from(Collections.singletonMap("key", "value"));

		assertThat(document.toJson()).isEqualTo("{\"key\":\"value\"}");
	}

	@Test // DATAES-628
	public void shouldParseDocumentFromJson() {

		Document document = Document.parse("{\"key\":\"value\"}");

		assertThat(document).containsEntry("key", "value");
	}

	@Test // DATAES-628
	public void shouldReturnContainsKey() {

		Document document = Document.create().append("string", "value").append("bool", true).append("int", 43)
				.append("long", 42L);

		assertThat(document.containsKey("string")).isTrue();
		assertThat(document.containsKey("not-set")).isFalse();
	}

	@Test // DATAES-628
	public void shouldReturnContainsValue() {

		Document document = Document.create().append("string", "value").append("bool", Arrays.asList(true, true, false))
				.append("int", 43).append("long", 42L);

		assertThat(document.containsValue("value")).isTrue();
		assertThat(document.containsValue(43)).isTrue();
		assertThat(document.containsValue(44)).isFalse();
		assertThat(document.containsValue(Arrays.asList(true, true, false))).isTrue();
	}

	@Test // DATAES-628
	public void shouldReturnTypedValue() {

		Document document = Document.create().append("string", "value").append("bool", true).append("int", 43)
				.append("long", 42L);

		assertThat(document.get("string")).isEqualTo("value");
		assertThat(document.getString("string")).isEqualTo("value");

		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.get("long", String.class));
	}

	@Test // DATAES-628
	public void shouldReturnTypedValueString() {

		Document document = Document.create().append("string", "value").append("bool", true).append("int", 43)
				.append("long", 42L);

		assertThat(document.getString("string")).isEqualTo("value");
		assertThat(document.getStringOrDefault("not-set", "default")).isEqualTo("default");
		assertThat(document.getStringOrDefault("not-set", () -> "default")).isEqualTo("default");

		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.getString("long"));
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.get("long", String.class));
	}

	@Test // DATAES-628
	public void shouldReturnTypedValueBoolean() {

		Document document = Document.create().append("string", "value").append("bool", true).append("int", 43)
				.append("long", 42L);

		assertThat(document.getBoolean("bool")).isTrue();
		assertThat(document.getBooleanOrDefault("not-set", true)).isTrue();
		assertThat(document.getBooleanOrDefault("not-set", () -> true)).isTrue();

		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.getString("bool"));
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.get("bool", String.class));
	}

	@Test // DATAES-628
	public void shouldReturnTypedValueInt() {

		Document document = Document.create().append("string", "value").append("bool", true).append("int", 43)
				.append("long", 42L);

		assertThat(document.getInt("int")).isEqualTo(43);
		assertThat(document.getIntOrDefault("not-set", 44)).isEqualTo(44);
		assertThat(document.getIntOrDefault("not-set", () -> 44)).isEqualTo(44);

		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.getString("int"));
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.get("int", String.class));
	}

	@Test // DATAES-628
	public void shouldReturnTypedValueLong() {

		Document document = Document.create().append("string", "value").append("bool", true).append("int", 43)
				.append("long", 42L);

		assertThat(document.getLong("long")).isEqualTo(42);
		assertThat(document.getLongOrDefault("not-set", 44)).isEqualTo(44);
		assertThat(document.getLongOrDefault("not-set", () -> 44)).isEqualTo(44);

		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.getString("long"));
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> document.get("long", String.class));
	}

	@Test // DATAES-628
	public void shouldApplyTransformer() {

		Document document = Document.create();
		document.setId("value");

		assertThat(document.transform(Document::getId)).isEqualTo("value");
	}

	@Test // DATAES-628
	public void shouldSetId() {

		Document document = Document.create();

		assertThat(document.hasId()).isFalse();
		assertThatIllegalStateException().isThrownBy(document::getId);

		document.setId("foo");
		assertThat(document.getId()).isEqualTo("foo");
	}

	@Test // DATAES-628
	public void shouldSetVersion() {

		Document document = Document.create();

		assertThat(document.hasVersion()).isFalse();
		assertThatIllegalStateException().isThrownBy(document::getVersion);

		document.setVersion(14);
		assertThat(document.getVersion()).isEqualTo(14);
	}

	@Test // DATAES-628
	public void shouldRenderToString() {

		Document document = Document.from(Collections.singletonMap("key", "value"));
		document.setId("123");
		document.setVersion(42);

		assertThat(document).hasToString("MapDocument@123#42 {\"key\":\"value\"}");
	}
}
