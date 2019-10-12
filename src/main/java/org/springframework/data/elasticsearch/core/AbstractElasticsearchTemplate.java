package org.springframework.data.elasticsearch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.MappingBuilder;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.util.StringUtils;

/**
 * AbstractElasticsearchTemplate
 *
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 */
public abstract class AbstractElasticsearchTemplate implements ElasticsearchOperations {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticsearchTemplate.class);

	protected ElasticsearchConverter elasticsearchConverter;

	protected ElasticsearchConverter createElasticsearchConverter() {
		MappingElasticsearchConverter mappingElasticsearchConverter = new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());
		mappingElasticsearchConverter.afterPropertiesSet();
		return mappingElasticsearchConverter;
	}

	protected String buildMapping(Class<?> clazz) {

		// load mapping specified in Mapping annotation if present
		if (clazz.isAnnotationPresent(Mapping.class)) {
			String mappingPath = clazz.getAnnotation(Mapping.class).mappingPath();
			if (!StringUtils.isEmpty(mappingPath)) {
				String mappings = ResourceUtil.readFileFromClasspath(mappingPath);
				if (!StringUtils.isEmpty(mappings)) {
					return mappings;
				}
			} else {
				LOGGER.info("mappingPath in @Mapping has to be defined. Building mappings using @Field");
			}
		}

		// build mapping from field annotations
		try {
			MappingBuilder mappingBuilder = new MappingBuilder(elasticsearchConverter);
			return mappingBuilder.buildPropertyMapping(clazz);
		} catch (Exception e) {
			throw new ElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
		}
	}

	@Override
	public ElasticsearchConverter getElasticsearchConverter() {
		return elasticsearchConverter;
	}
}
