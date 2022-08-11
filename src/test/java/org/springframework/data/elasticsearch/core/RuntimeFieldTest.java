package org.springframework.data.elasticsearch.core;


import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author cdalxndr
 */
class RuntimeFieldTest {

	@Test //#2267
	@DisplayName("should return mapping with script")
	void testMapping() {
		RuntimeField runtimeField = new RuntimeField("name", "double", "myscript");
		Map<String, Object> mapping = runtimeField.getMapping();
		assertThat(mapping).containsEntry("type", "double")
				.containsEntry("script", "myscript");
	}

	@Test //#2267
	@DisplayName("should return mapping without script")
	void testMappingNoScript() {
		RuntimeField runtimeField = new RuntimeField("name", "double");
		Map<String, Object> mapping = runtimeField.getMapping();
		assertThat(mapping).containsEntry("type", "double")
				.doesNotContainKey("script");
	}

}