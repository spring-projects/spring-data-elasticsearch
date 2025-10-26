package org.springframework.data.elasticsearch.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;

public class IndexSettingsELCIntegrationTests extends IndexSettingsIntegrationTests {
	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		public SpelSettingPath spelSettingPath() {
			return new SpelSettingPath();
		}
	}
}
