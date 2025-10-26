package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;

/**
 * IndexSettings test that need an regular conext setup for SpEL resolution for example.
 */
@SpringIntegrationTest
public abstract class IndexSettingsIntegrationTests {

	@Autowired protected ElasticsearchOperations operations;

	@Test // #3187
	@DisplayName("should evaluate SpEL expression in settingPath")
	void shouldEvaluateSpElExpressionInSettingPath() {

		var settingPath = operations.getElasticsearchConverter().getMappingContext()
				.getRequiredPersistentEntity(SettingPathWithSpel.class).settingPath();

		assertThat(settingPath).isEqualTo(SpelSettingPath.SETTING_PATH);
	}

	protected static class SpelSettingPath {
		public static String SETTING_PATH = "test-setting-path";

		public String settingPath() {
			return SETTING_PATH;
		}
	}

	@Document(indexName = "foo")
	@Setting(settingPath = "#{@spelSettingPath.settingPath}")
	private static class SettingPathWithSpel {
		@Nullable
		@Id String id;
	}

}
