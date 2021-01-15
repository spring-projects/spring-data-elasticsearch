/*
 * (c) Copyright 2021 sothawo
 */
package org.springframework.data.elasticsearch.core.mapping;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@ContextConfiguration(classes = { FieldNamingStrategyIntegrationTemplateTest.Config.class })
public class FieldNamingStrategyIntegrationTemplateTest extends FieldNamingStrategyIntegrationTest {

	@Configuration
	static class Config extends ElasticsearchTemplateConfiguration {

		@Override
		protected FieldNamingStrategy fieldNamingStrategy() {
			return new SnakeCaseFieldNamingStrategy();
		}
	}

}
