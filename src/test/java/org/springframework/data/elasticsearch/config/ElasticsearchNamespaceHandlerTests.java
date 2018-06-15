/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.config;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.client.RestClientFactoryBean;
import org.springframework.data.elasticsearch.client.TransportClientFactoryBean;
import org.springframework.data.elasticsearch.repositories.sample.SampleElasticsearchRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Don Wellington
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("namespace.xml")
public class ElasticsearchNamespaceHandlerTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void shouldCreateTransportClient() {
		assertThat(context.getBean(TransportClientFactoryBean.class), is(notNullValue()));
		assertThat(context.getBean(TransportClientFactoryBean.class), is(instanceOf(TransportClientFactoryBean.class)));
	}

	@Test
	public void shouldCreateRepository() {
		assertThat(context.getBean(TransportClientFactoryBean.class), is(notNullValue()));
		assertThat(context.getBean(SampleElasticsearchRepository.class),
				is(instanceOf(SampleElasticsearchRepository.class)));
	}
	
	@Test
	public void shouldCreateRestClient() {
		assertThat(context.getBean(RestClientFactoryBean.class), is(notNullValue()));
		assertThat(context.getBean(RestClientFactoryBean.class), is(instanceOf(RestClientFactoryBean.class)));
	}
}
