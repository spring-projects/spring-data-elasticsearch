package org.springframework.data.elasticsearch.core.index;

import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchRestTemplateConfiguration;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author George Popides
 */
@ContextConfiguration(classes = { ElasticsearchRestTemplateConfiguration.class })
public class IndexOperationRestTemplateIntegrationTests extends IndexOperationIntegrationTests {}
