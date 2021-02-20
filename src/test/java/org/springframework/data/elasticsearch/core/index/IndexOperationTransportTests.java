package org.springframework.data.elasticsearch.core.index;

import org.springframework.data.elasticsearch.junit.jupiter.ElasticsearchTemplateConfiguration;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author George Popides
 */

@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
public class IndexOperationTransportTests extends IndexOperationTests {
}
