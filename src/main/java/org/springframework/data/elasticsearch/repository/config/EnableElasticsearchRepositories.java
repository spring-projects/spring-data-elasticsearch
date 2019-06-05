/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.config;

import java.lang.annotation.*;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactoryBean;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Annotation to enable Elasticsearch repositories. Will scan the package of the annotated configuration class for
 * Spring Data repositories by default.
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Kevin Leturc
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ElasticsearchRepositoriesRegistrar.class)
public @interface EnableElasticsearchRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableElasticsearchRepositories("org.my.pkg")} instead of
	 * {@code @EnableElasticsearchRepositories(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to text-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
	 * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
	 * for {@code PersonRepositoryImpl}.
	 *
	 * @return
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures the location of where to find the Spring Data named queries properties file. Will default to
	 * {@code META-INFO/elasticsearch-named-queries.properties}.
	 *
	 * @return
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link org.springframework.data.repository.query.QueryLookupStrategy} to be used for lookup
	 * queries for query methods. Defaults to
	 * {@link org.springframework.data.repository.query.QueryLookupStrategy.Key#CREATE_IF_NOT_FOUND}.
	 *
	 * @return
	 */
	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

	/**
	 * Returns the {@link org.springframework.beans.factory.FactoryBean} class to be used for each repository instance.
	 * Defaults to {@link ElasticsearchRepositoryFactoryBean}.
	 *
	 * @return
	 */
	Class<?> repositoryFactoryBeanClass() default ElasticsearchRepositoryFactoryBean.class;

	/**
	 * Configure the repository base class to be used to create repository proxies for this particular configuration.
	 *
	 * @return
	 */
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	// Elasticsearch specific configuration

	/**
	 * Configures the name of the {@link ElasticsearchTemplate} bean definition to be used to create repositories
	 * discovered through this annotation. Defaults to {@code elasticsearchTemplate}.
	 *
	 * @return
	 */
	String elasticsearchTemplateRef() default "elasticsearchTemplate";

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
	 * repositories infrastructure.
	 */
	boolean considerNestedRepositories() default false;
}
