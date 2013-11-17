Spring Data Elasticsearch
========================= 

Spring Data implementation for ElasticSearch

Spring Data makes it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services as well as provide improved support for relational database technologies.

The Spring Data Elasticsearch project provides integration with the [elasticsearch](http://www.elasticsearch.org/) search engine.

Guide
------------

* [Reference Documentation](https://github.com/BioMedCentralLtd/spring-data-elasticsearch/blob/master/site/reference.zip?raw=true)
* [PDF Documentation](https://github.com/BioMedCentralLtd/spring-data-elasticsearch/blob/master/site/reference/pdf/spring-data-elasticsearch-reference.pdf?raw=true)
* [API Documentation](https://github.com/BioMedCentralLtd/spring-data-elasticsearch/blob/master/site/apidocs.zip?raw=true)
* [Spring Data Project](http://www.springsource.org/spring-data)
* [Sample Test Application](https://github.com/BioMedCentralLtd/spring-data-elasticsearch-sample-application)

Test Coverage
-------------
* Class 92%
* Method 80%
* Line 74%
* Block 74%

[Emma Test Coverage Report] (https://github.com/BioMedCentralLtd/spring-data-elasticsearch/blob/master/site/emma.zip?raw=true)

Quick Start
-----------
Wiki page for [Getting Started] (https://github.com/spring-projects/spring-data-elasticsearch/wiki/How-to-start-with-spring-data-elasticsearch)

### Dependency
```java
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-elasticsearch</artifactId>
    <version>1.0.0.BUILD-SNAPSHOT</version>
</dependency> 
```

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
    public interface BookRepository extends Repository<Book, String> {

	//Equivalent Json Query will be "{ "bool" : { "must" :[{ "field" : {"name" : "?"} },{ "field" : {"price" : "?"} }]} }"
        List<Book> findByNameAndPrice(String name, Integer price);

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

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
        .withQuery(fieldQuery("id", documentId))
        .build();
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

### Geo indexing and request

You can make request using geo_distance filter. This can be done using GeoPoint object.

First, here is a sample of an entity with a GeoPoint field (location)

```java
@Document(indexName = "test-geo-index", type = "geo-class-point-type")
public class AuthorMarkerEntity {

    @Id
    private String id;
    private String name;

    private GeoPoint location;

    private AuthorMarkerEntity(){
    }

    public AuthorMarkerEntity(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }
}
```

Indexing your entity with a geo-point : 
```java
elasticsearchTemplate.createIndex(AuthorMarkerEntity.class);
elasticsearchTemplate.refresh(AuthorMarkerEntity.class, true);
elasticsearchTemplate.putMapping(AuthorMarkerEntity.class);

List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
indexQueries.add(new AuthorMarkerEntityBuilder("1").name("Franck Marchand").location(45.7806d, 3.0875d).buildIndex());
indexQueries.add(new AuthorMarkerEntityBuilder("2").name("Mohsin Husen").location(51.5171d, 0.1062d).buildIndex());
indexQueries.add(new AuthorMarkerEntityBuilder("3").name("Rizwan Idrees").location(51.5171d, 0.1062d).buildIndex());
elasticsearchTemplate.bulkIndex(indexQueries);
```

For your information : 
- Clermont-Ferrand : 45.7806, 3.0875
- London : 51.5171, 0.1062

So, if you want to search for authors who are located within 20 kilometers around Clermont-Ferrand, here is the way to build your query :

```java
CriteriaQuery geoLocationCriteriaQuery = new CriteriaQuery(
                new Criteria("location").within(new GeoPoint(45.7806d, 3.0875d), "20km"));
List<AuthorMarkerEntity> geoAuthorsForGeoCriteria = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery, AuthorMarkerEntity.class);
```

This example should only return one author named "Franck Marchand".

You can even combine with other criteria (e.g. author name) :

Here, we're looking for authors located within 20 kilometers around London AND named "Mohsin Husen" : 

```java
CriteriaQuery geoLocationCriteriaQuery2 = new CriteriaQuery(
                new Criteria("name").is("Mohsin Husen").and("location").within(new GeoPoint(51.5171d, 0.1062d), "20km"));
List<AuthorMarkerEntity> geoAuthorsForGeoCriteria2 = elasticsearchTemplate.queryForList(geoLocationCriteriaQuery2, AuthorMarkerEntity.class);
```

This example should only return one author named "Mohsin Husen".


### XML Namespace

You can set up repository scanning via xml configuration, which will happily create your repositories.

Using Node Client

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:elasticsearch="http://www.springframework.org/schema/data/elasticsearch"
       xsi:schemaLocation="http://www.springframework.org/schema/data/elasticsearch http://www.springframework.org/schema/data/elasticsearch/spring-elasticsearch-1.0.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.1.xsd">

    <elasticsearch:node-client id="client" local="true"/>

    <bean name="elasticsearchTemplate" class="org.springframework.data.elasticsearch.core.ElasticsearchTemplate">
        <constructor-arg name="client" ref="client"/>
    </bean>

</beans>
```

Using Transport Client

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:elasticsearch="http://www.springframework.org/schema/data/elasticsearch"
       xsi:schemaLocation="http://www.springframework.org/schema/data/elasticsearch http://www.springframework.org/schema/data/elasticsearch/spring-elasticsearch-1.0.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.1.xsd">

    <elasticsearch:repositories base-package="com.xyz.acme"/>

    <elasticsearch:transport-client id="client" cluster-nodes="localhost:9300,someip:9300" />

    <bean name="elasticsearchTemplate" class="org.springframework.data.elasticsearch.core.ElasticsearchTemplate">
        <constructor-arg name="client" ref="client"/>
    </bean>

</beans>
```    

### Contact Details

* Rizwan Idrees (rizwan.idrees@biomedcentral.com)
* Abdul Waheed (abdul.mohammed@biomedcentral.com)
* Mohsin Husen (mohsin.husen@biomedcentral.com)
