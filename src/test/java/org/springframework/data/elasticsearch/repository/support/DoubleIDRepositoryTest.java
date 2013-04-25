/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repository.support;

import static org.elasticsearch.index.query.QueryBuilders.fieldQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.DoubleIDEntity;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repositories.DoubleIDRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/simple-repository-test.xml")
public class DoubleIDRepositoryTest {


  @Resource
  private DoubleIDRepository repository;


  @Before
  public void before(){
      repository.deleteAll();
  }

  @Test
  public void shouldDoBulkIndexDocument(){
      //given
      Double documentId1 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity1 = new DoubleIDEntity();
      sampleEntity1.setId(documentId1);
      sampleEntity1.setMessage("some message");
      sampleEntity1.setVersion(System.currentTimeMillis());

      Double documentId2 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity2 = new DoubleIDEntity();
      sampleEntity2.setId(documentId2);
      sampleEntity2.setMessage("some message");
      sampleEntity2.setVersion(System.currentTimeMillis());

      //when
      repository.save(Arrays.asList(sampleEntity1, sampleEntity2));
      //then
      DoubleIDEntity entity1FromElasticSearch =  repository.findOne(documentId1);
      assertThat(entity1FromElasticSearch, is(notNullValue()));

      DoubleIDEntity entity2FromElasticSearch =  repository.findOne(documentId2);
      assertThat(entity2FromElasticSearch, is(notNullValue()));
  }

  @Test
  public void shouldSaveDocument(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("some message");
      sampleEntity.setVersion(System.currentTimeMillis());
      //when
      repository.save(sampleEntity);
      //then
      DoubleIDEntity entityFromElasticSearch =  repository.findOne(documentId);
      assertThat(entityFromElasticSearch, is(notNullValue()));
  }

  @Test
  public void shouldFindDocumentById(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("some message");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);
      //when
      DoubleIDEntity entityFromElasticSearch = repository.findOne(documentId);
      //then
      assertThat(entityFromElasticSearch, is(notNullValue()));
      assertThat(sampleEntity, is((equalTo(sampleEntity))));
  }

  @Test
  public void shouldReturnCountOfDocuments(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("some message");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);
      //when
      Long count = repository.count();
      //then
      assertThat(count, is(greaterThanOrEqualTo(1L)));
  }

  @Test
  public void shouldFindAllDocuments(){
      //when
      Iterable<DoubleIDEntity> results = repository.findAll();
      //then
      assertThat(results, is(notNullValue()));
  }

  @Test
  public void shouldDeleteDocument(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("some message");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);
      //when
      repository.delete(documentId);
      //then
      DoubleIDEntity entityFromElasticSearch = repository.findOne(documentId);
      assertThat(entityFromElasticSearch, is(nullValue()));
  }

  @Test
  public void shouldSearchDocumentsGivenSearchQuery(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("some test message");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);

      SearchQuery query = new NativeSearchQueryBuilder()
              .withQuery(termQuery("message", "test"))
              .build();
      //when
      Page<DoubleIDEntity> page = repository.search(query);
      //then
      assertThat(page, is(notNullValue()));
      assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
  }

  @Test
  public void shouldSearchDocumentsGivenElasticsearchQuery(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("hello world.");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);
      //when
      Page<DoubleIDEntity> page = repository.search(termQuery("message", "world"), new PageRequest(0,50));
      //then
      assertThat(page, is(notNullValue()));
      assertThat(page.getNumberOfElements(), is(greaterThanOrEqualTo(1)));
  }

  @Test
  @Ignore
  public  void shouldFindAllByIdQuery(){
      //todo : find solution for findAll(Iterable<Ids> ids)
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("hello world.");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);

      Double documentId2 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity2 = new DoubleIDEntity();
      sampleEntity2.setId(documentId2);
      sampleEntity2.setMessage("hello world.");
      sampleEntity2.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity2);

      //when
      Iterable<DoubleIDEntity> sampleEntities=repository.findAll(Arrays.asList(documentId,documentId2));

      //then
      assertNotNull("sample entities cant be null..", sampleEntities);
  }

  @Test
  public void shouldSaveIterableEntities(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity1 = new DoubleIDEntity();
      sampleEntity1.setId(documentId);
      sampleEntity1.setMessage("hello world.");
      sampleEntity1.setVersion(System.currentTimeMillis());

      Double documentId2 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity2 = new DoubleIDEntity();
      sampleEntity2.setId(documentId2);
      sampleEntity2.setMessage("hello world.");
      sampleEntity2.setVersion(System.currentTimeMillis());

      Iterable<DoubleIDEntity> sampleEntities = Arrays.asList(sampleEntity1,sampleEntity2);
      //when
      repository.save(sampleEntities);
      //then
      Page<DoubleIDEntity> entities = repository.search(fieldQuery("id", documentId), new PageRequest(0, 50));
      assertNotNull(entities);
  }

  @Test
  public void shouldReturnTrueGivenDocumentWithIdExists(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("hello world.");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);

      //when
      boolean exist = repository.exists(documentId);

      //then
      assertEquals(exist, true);
  }

  @Test
  public void shouldReturnResultsForGivenSearchQuery(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("hello world.");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);
      //when
      SearchQuery searchQuery = new NativeSearchQueryBuilder()
              .withQuery(fieldQuery("id",documentId))
              .build();
      Page<DoubleIDEntity> sampleEntities= repository.search(searchQuery);
      //then
      assertThat(sampleEntities.getTotalElements(), equalTo(1L));
  }

  @Test
  public void shouldDeleteAll(){
      //when
      repository.deleteAll();
      //then
      SearchQuery searchQuery = new NativeSearchQueryBuilder()
              .withQuery(matchAllQuery())
              .build();
      Page<DoubleIDEntity> sampleEntities= repository.search(searchQuery);
      assertThat(sampleEntities.getTotalElements(), equalTo(0L));
  }

  @Test
  public void shouldDeleteEntity(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("hello world.");
      sampleEntity.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity);
      //when
      repository.delete(sampleEntity);
      //then
      SearchQuery searchQuery = new NativeSearchQueryBuilder()
              .withQuery(fieldQuery("id", documentId))
              .build();
      Page<DoubleIDEntity> sampleEntities= repository.search(searchQuery);
      assertThat(sampleEntities.getTotalElements(),equalTo(0L));
  }

  @Test
  public void shouldReturnIterableEntities(){
      //given
      Double documentId1 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity1 = new DoubleIDEntity();
      sampleEntity1.setId(documentId1);
      sampleEntity1.setMessage("hello world.");
      sampleEntity1.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity1);

      Double documentId2 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity2 = new DoubleIDEntity();
      sampleEntity2.setId(documentId2);
      sampleEntity2.setMessage("hello world.");
      sampleEntity2.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity2);

      //when
      Iterable<DoubleIDEntity> sampleEntities = repository.search(fieldQuery("id",documentId1));
      //then
      assertNotNull("sample entities cant be null..", sampleEntities);
  }

  @Test
  public void shouldDeleteIterableEntities(){
      //given
      Double documentId1 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity1 = new DoubleIDEntity();
      sampleEntity1.setId(documentId1);
      sampleEntity1.setMessage("hello world.");
      sampleEntity1.setVersion(System.currentTimeMillis());

      Double documentId2 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity2 = new DoubleIDEntity();
      sampleEntity2.setId(documentId2);
      sampleEntity2.setMessage("hello world.");
      sampleEntity2.setVersion(System.currentTimeMillis());
      repository.save(sampleEntity2);

      Iterable<DoubleIDEntity> sampleEntities = Arrays.asList(sampleEntity2,sampleEntity2);
      //when
      repository.delete(sampleEntities);
      //then
      assertThat(repository.findOne(documentId1),is(nullValue()));
      assertThat(repository.findOne(documentId2),is(nullValue()));
  }

  @Test
  public void shouldIndexEntity(){
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setVersion(System.currentTimeMillis());
      sampleEntity.setMessage("some message");
      //when
      repository.index(sampleEntity);
      //then
      Page<DoubleIDEntity> entities = repository.search(fieldQuery("id", documentId), new PageRequest(0,50));
      assertThat(entities.getTotalElements(),equalTo(1L));
  }

  @Test
  public void shouldSortByGivenField(){
      //todo
      //given
      Double documentId = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity = new DoubleIDEntity();
      sampleEntity.setId(documentId);
      sampleEntity.setMessage("A. hello world.");
      repository.save(sampleEntity);

      Double documentId2 = RandomUtils.nextDouble();
      DoubleIDEntity sampleEntity2 = new DoubleIDEntity();
      sampleEntity2.setId(documentId2);
      sampleEntity2.setMessage("B.hello world.");
      repository.save(sampleEntity2);
      //when
      Iterable<DoubleIDEntity> sampleEntities=repository.findAll(new Sort(new Sort.Order(Sort.Direction.ASC,"message")));
      //then
      assertThat(sampleEntities,is(notNullValue()));
  }


  @Test
  public void shouldReturnSimilarEntities(){
      //given
      String sampleMessage = "So we build a web site or an application and want to add search to it, " +
              "and then it hits us: getting search working is hard. We want our search solution to be fast," +
              " we want a painless setup and a completely free search schema, we want to be able to index data simply using JSON over HTTP, " +
              "we want our search server to be always available, we want to be able to start with one machine and scale to hundreds, " +
              "we want real-time search, we want simple multi-tenancy, and we want a solution that is built for the cloud.";



      List<DoubleIDEntity> sampleEntities = createSampleEntitiesWithMessage(sampleMessage, 30);
      repository.save(sampleEntities);

      //when
      Page<DoubleIDEntity> results = repository.searchSimilar(sampleEntities.get(0));

      //then
      assertThat(results.getTotalElements(), is(greaterThanOrEqualTo(1L)));
  }

  private static List<DoubleIDEntity> createSampleEntitiesWithMessage(String message, int numberOfEntities){
      List<DoubleIDEntity> sampleEntities = new ArrayList<DoubleIDEntity>();
      for(int i = 0; i < numberOfEntities; i++){
          Double documentId = RandomUtils.nextDouble();
          DoubleIDEntity sampleEntity = new DoubleIDEntity();
          sampleEntity.setId(documentId);
          sampleEntity.setMessage(message);
          sampleEntity.setVersion(System.currentTimeMillis());
          sampleEntities.add(sampleEntity);
      }
      return sampleEntities;
  }
  
}
