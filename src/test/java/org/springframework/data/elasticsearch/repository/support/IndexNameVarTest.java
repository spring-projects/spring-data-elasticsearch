/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.elasticsearch.repository.support;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.Chat;
import org.springframework.data.elasticsearch.repositories.chat.ChatRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author zzt
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/chat-repository-test.xml")
public class IndexNameVarTest {

	private static final ExecutorService service = Executors.newCachedThreadPool();
	private static final String first = "2017-06";
	private static final String second = "2017-07";
	@Autowired private ChatRepository repository;
	@Autowired private IndexNameVar indexNameVar;
	@Autowired private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void setup() {
		elasticsearchTemplate.deleteIndex(Chat.class);
		elasticsearchTemplate.createIndex(Chat.class);
		elasticsearchTemplate.putMapping(Chat.class);
		elasticsearchTemplate.refresh(Chat.class);
	}

	@Test // DATAES-233
	public void setVars() throws Exception {
		// given
		indexNameVar.setVars(first);
		repository.save(new Chat("1", "s1", "s2", "hello!"));
		repository.save(new Chat("2", "s2", "s1", "nice to meet you"));
		repository.save(new Chat("3", "s1", "s2", "me too"));
		indexNameVar.setVars(second);
		repository.save(new Chat("10", "s1", "s2", "what are you doing"));
		repository.save(new Chat("11", "s2", "s1", "having meal"));
		Callable<Void> r1 = () -> {
			// when
			indexNameVar.setVars(first);
			Page<Chat> s1 = repository.findBySender("s1", new PageRequest(0, 10));
			// then
			assertThat(s1.getTotalElements() == 2, is(true));
			System.out.println(s1.getContent());
			return null;
		};
		Callable<Void> r2 = () -> {
			// when
			indexNameVar.setVars(second);
			Page<Chat> s1 = repository.findBySender("s1", new PageRequest(0, 10));
			// then
			assertThat(s1.getTotalElements() == 1, is(true));
			System.out.println(s1.getContent());
			return null;
		};
		service.invokeAll(Arrays.asList(r1, r2));
	}

}
