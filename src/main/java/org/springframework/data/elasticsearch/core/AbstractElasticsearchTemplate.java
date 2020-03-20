package org.springframework.data.elasticsearch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * AbstractElasticsearchTemplate
 * 
 * @author Sascha Woo
 */
public abstract class AbstractElasticsearchTemplate {

	static final Integer INDEX_MAX_RESULT_WINDOW = 10_000;

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticsearchTemplate.class);

	protected ElasticsearchConverter elasticsearchConverter;

	public AbstractElasticsearchTemplate(ElasticsearchConverter elasticsearchConverter) {

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null.");
		this.elasticsearchConverter = elasticsearchConverter;
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

}
