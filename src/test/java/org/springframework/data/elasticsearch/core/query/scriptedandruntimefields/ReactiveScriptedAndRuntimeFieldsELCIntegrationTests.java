package org.springframework.data.elasticsearch.core.query.scriptedandruntimefields;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.junit.jupiter.ReactiveElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @since 5.2
 */
@ContextConfiguration(classes = ReactiveScriptedAndRuntimeFieldsELCIntegrationTests.Config.class)
public class ReactiveScriptedAndRuntimeFieldsELCIntegrationTests
		extends ReactiveScriptedAndRuntimeFieldsIntegrationTests {

	@Configuration
	@Import({ ReactiveElasticsearchTemplateConfiguration.class })
	@EnableReactiveElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("reactive-scripted-runtime");
		}
	}
}
