spring-data-elasticsearch
========================= 

Spring Data implementation for ElasticSearch

[![Build Status](https://secure.travis-ci.org/BioMedCentralLtd/spring-data-elasticsearch.png)](http://travis-ci.org/BioMedCentralLtd/spring-data-elasticsearch)

Spring Data makes it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services as well as provide improved support for relational database technologies.

The Spring Data Elasticsearch project provides integration with the [elasticsearch](http://www.elasticsearch.org/) search engine.

Guide
------------

* [Reference Documentation](https://github.com/BioMedCentralLtd/spring-data-elasticsearch/tree/master/site/reference/html)
* [PDF Documentation](https://github.com/BioMedCentralLtd/spring-data-elasticsearch/blob/master/site/reference/pdf/spring-data-elasticsearch-reference.pdf?raw=true)
* [API Documentation](https://github.com/BioMedCentralLtd/spring-data-elasticsearch/tree/master/site/apidocs)
* [Spring Data Project](http://www.springsource.org/spring-data)


Test Coverage
-------------
* Class 92%
* Method 80%
* Line 74%
* Block 74%

*[Emma Test Coverage Report] (https://github.com/BioMedCentralLtd/spring-data-elasticsearch/tree/master/site/emma/)

Quick Start
-----------

### ElasticsearchTemplate
ElasticsearchTemplate is the central support class for elasticsearch operations.


### ElasticsearchRepository
A default implementation of ElasticsearchRepository, aligning to the generic Repository Interfaces, is provided. Spring can do the Repository implementation for you depending on method names in the interface definition.

The ElasticsearchCrudRepository extends PagingAndSortingRepository

```java
    public interface ElasticsearchCrudRepository<T, ID extends Serializable> extends ElasticsearchRepository<T, ID>, PagingAndSortingRepository<T, ID> {
    }
```

Extending ElasticsearchRepository for custom methods

```java
    public interface BookRepository extends Repository&lt;Book, String&gt; {

        //Equivalent Json Query will be "{ "bool" : { "must" :[{ "field" : {"name" : "?"} },{ "field" : {"price" : "?"} }]} }"
        List<Book>; findByNameAndPrice(String name, Integer price);

        //Equivalent Json Query will be "{"bool" : {"should" : [ {"field" : "name" : "?"}}, {"field" : {"price" : "?"}} ]}}"
        List<Book> findByNameOrPrice(String name, Integer price);

        //Equivalent Json Query will be "{"bool" : {"must" : {"field" : {"name" : "?"}}}}"
        Page<Book> findByName(String name,Pageable page);

        //Equivalent Json Query will be "{"bool" : {"must_not" : {"field" : {"name" : "?"}}}}"
        Page<Book> findByNameNot(String name,Pageable page);

        //Equivalent Json Query will be "{"bool" : {"must" : {"range" : {"price" : {"from" : ?,"to" : ?,"include_lower" : true,"include_upper" : true}}}}}"
        Page<Book> findByPriceBetween(int price,Pageable page);


        //Equivalent Json Query will be "{"bool" : {"must" : {"field" : {"name" : {"query" : "?*","analyze_wildcard" : true}}}}"
        Page<Book> findByNameLike(String name,Pageable page);


        @Query("{\"bool\" : {\"must\" : {\"field\" : {\"message\" : \"?0\"}}}}")
        Page<Book> findByMessage(String message, Pageable pageable);
    }
```

Indexing a single document using Elasticsearch Template

```java
        String documentId = "123456";
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId(documentId);
        indexQuery.setObject(sampleEntity);
        elasticsearchTemplate.index(indexQuery);
```

Indexing multiple Document(bulk index) using Elasticsearch Template

```java
        @Autowired
        private ElasticsearchTemplate elasticsearchTemplate;

        List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
        //first document
        String documentId = "123456";
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("some message");

        IndexQuery indexQuery1 = new IndexQuery();
        indexQuery1.setId(documentId);
        indexQuery1.setObject(sampleEntity1);
        indexQueries.add(indexQuery1);

        //second document
        String documentId2 = "123457";
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("some message");
        IndexQuery indexQuery2 = new IndexQuery();
        indexQuery2.setId(documentId2);
        indexQuery2.setObject(sampleEntity2);
        indexQueries.add(indexQuery2);
        //bulk index
        elasticsearchTemplate.bulkIndex(indexQueries);
```

Searching entities using Elasticsearch Template

```java
        @Autowired
        private ElasticsearchTemplate elasticsearchTemplate;

        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setElasticsearchQuery(fieldQuery("id", documentId));
        Page<SampleEntity> sampleEntities = elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
```

Indexing a single document with Repository

```java
        @Resource
        private SampleElasticsearchRepository repository;

        String documentId = "123456";
        SampleEntity sampleEntity = new SampleEntity();
        sampleEntity.setId(documentId);
        sampleEntity.setMessage("some message");

        repository.save(sampleEntity);
```

Indexing multiple Document(bulk index) using Repository

```java
        @Resource
        private SampleElasticsearchRepository repository;

        String documentId = "123456";
        SampleEntity sampleEntity1 = new SampleEntity();
        sampleEntity1.setId(documentId);
        sampleEntity1.setMessage("some message");

        String documentId2 = "123457"
        SampleEntity sampleEntity2 = new SampleEntity();
        sampleEntity2.setId(documentId2);
        sampleEntity2.setMessage("test message");

        List<SampleEntity> sampleEntities = Arrays.asList(sampleEntity1, sampleEntity2);

        //bulk index
        repository.save(sampleEntities);
```

### XML Namespace

You can set up repository scanning via xml configuration, which will happily create your repositories.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:elasticsearch="http://www.springframework.org/schema/data/elasticsearch"
       xsi:schemaLocation="http://www.springframework.org/schema/data/elasticsearch http://www.springframework.org/schema/data/elasticsearch/spring-elasticsearch-1.0.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <elasticsearch:node-client id="client" local="true"/>

    <bean name="elasticsearchTemplate" class="org.springframework.data.elasticsearch.core.ElasticsearchTemplate">
        <constructor-arg name="client" ref="client"/>
    </bean>

</beans>
```
