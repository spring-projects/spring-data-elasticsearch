/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ProcessBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;

/**
 * CDI extension to export Elasticsearch repositories.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ElasticsearchRepositoryExtension extends CdiRepositoryExtensionSupport {

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchRepositoryExtension.class);

	private final Map<Set<Annotation>, Bean<ElasticsearchOperations>> elasticsearchOperationsMap = new HashMap<>();

	@SuppressWarnings("unchecked")
	<T> void processBean(@Observes ProcessBean<T> processBean) {

		Bean<T> bean = processBean.getBean();

		for (Type type : bean.getTypes()) {
			if (type instanceof Class<?> && ElasticsearchOperations.class.isAssignableFrom((Class<?>) type)) {

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Discovered %s with qualifiers %s.", ElasticsearchOperations.class.getName(),
							bean.getQualifiers()));
				}

				elasticsearchOperationsMap.put(new HashSet<>(bean.getQualifiers()), ((Bean<ElasticsearchOperations>) bean));
			}
		}
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
		for (Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {

			Class<?> repositoryType = entry.getKey();
			Set<Annotation> qualifiers = entry.getValue();

			CdiRepositoryBean<?> repositoryBean = createRepositoryBean(repositoryType, qualifiers, beanManager);

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Registering bean for %s with qualifiers %s.", repositoryType.getName(), qualifiers));
			}

			registerBean(repositoryBean);
			afterBeanDiscovery.addBean(repositoryBean);
		}
	}

	private <T> CdiRepositoryBean<T> createRepositoryBean(Class<T> repositoryType, Set<Annotation> qualifiers,
			BeanManager beanManager) {

		Bean<ElasticsearchOperations> elasticsearchOperations = this.elasticsearchOperationsMap.get(qualifiers);

		if (elasticsearchOperations == null) {
			throw new UnsatisfiedResolutionException(String.format("Unable to resolve a bean for '%s' with qualifiers %s.",
					ElasticsearchOperations.class.getName(), qualifiers));
		}

		return new ElasticsearchRepositoryBean<>(elasticsearchOperations, qualifiers, repositoryType, beanManager,
				Optional.of(getCustomImplementationDetector()));
	}
}
