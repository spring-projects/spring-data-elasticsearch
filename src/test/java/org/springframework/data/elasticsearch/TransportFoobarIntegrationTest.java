package org.springframework.data.elasticsearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.data.elasticsearch.junit.jupiter.IntegrationtestEnvironment;
import org.springframework.test.context.ContextConfiguration;

/**
 * This class should only run when the cluster is an Elasticsearch cluster.
 *
 * @author Peter-Josef Meisch
 */
@EnabledIfSystemProperty(named = IntegrationtestEnvironment.SYSTEM_PROPERTY, matches = "(?i)elasticsearch")
@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
@DisplayName("foobar integration with transport client")
public class TransportFoobarIntegrationTest extends FoobarIntegrationTest {}
