/*
 * Copyright 2019-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.convert;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.domain.Range;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.ValueConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.geo.GeoJsonEntity;
import org.springframework.data.elasticsearch.core.geo.GeoJsonGeometryCollection;
import org.springframework.data.elasticsearch.core.geo.GeoJsonLineString;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiLineString;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiPoint;
import org.springframework.data.elasticsearch.core.geo.GeoJsonMultiPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPolygon;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Unit tests for {@link MappingElasticsearchConverter}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Konrad Kurdej
 * @author Roman Puchkovskiy
 * @author Sascha Woo
 */
public class MappingElasticsearchConverterUnitTests {

	static final String JSON_STRING = """
			{
			  "_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$Car",
			  "name": "Grat",
			  "model": "Ford"
			}
			""";
	static final String CAR_MODEL = "Ford";
	static final String CAR_NAME = "Grat";
	MappingElasticsearchConverter mappingElasticsearchConverter;

	Person sarahConnor;
	Person kyleReese;
	Person t800;

	Inventory gun = new Gun("Glock 19", 33);
	Inventory grenade = new Grenade("40 mm");
	Inventory rifle = new Rifle("AR-18 Assault Rifle", 3.17, 40);
	Inventory shotGun = new ShotGun("Ithaca 37 Pump Shotgun");

	Address observatoryRoad;
	Place bigBunsCafe;

	Document sarahAsMap;
	Document t800AsMap;
	Document kyleAsMap;
	Document gratiotAveAsMap;
	Document locationAsMap;
	Document gunAsMap;
	Document grenadeAsMap;
	Document rifleAsMap;
	Document shotGunAsMap;
	Document bigBunsCafeAsMap;
	Document notificationAsMap;

	@BeforeEach
	public void init() {

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(Collections.singleton(Rifle.class));
		mappingContext.afterPropertiesSet();

		mappingElasticsearchConverter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
		mappingElasticsearchConverter.setConversions(
				new ElasticsearchCustomConversions(Arrays.asList(new ShotGunToMapConverter(), new MapToShotGunConverter())));
		mappingElasticsearchConverter.afterPropertiesSet();

		sarahConnor = new Person();
		sarahConnor.id = "sarah";
		sarahConnor.name = "Sarah Connor";
		sarahConnor.gender = Gender.MAN;

		kyleReese = new Person();
		kyleReese.id = "kyle";
		kyleReese.gender = Gender.MAN;
		kyleReese.name = "Kyle Reese";

		t800 = new Person();
		t800.id = "t800";
		t800.name = "T-800";
		t800.gender = Gender.MACHINE;

		t800AsMap = Document.create();
		t800AsMap.put("id", "t800");
		t800AsMap.put("name", "T-800");
		t800AsMap.put("gender", "MACHINE");
		t800AsMap.put("_class",
				"org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$Person");

		observatoryRoad = new Address();
		observatoryRoad.city = "Los Angeles";
		observatoryRoad.street = "2800 East Observatory Road";
		observatoryRoad.location = new Point(-118.3026284D, 34.118347D);

		bigBunsCafe = new Place();
		bigBunsCafe.setName("Big Buns Cafe");
		bigBunsCafe.setCity("Los Angeles");
		bigBunsCafe.setStreet("15 South Fremont Avenue");
		bigBunsCafe.setLocation(new Point(-118.1545845D, 34.0945637D));

		sarahAsMap = Document.create();
		sarahAsMap.put("id", "sarah");
		sarahAsMap.put("name", "Sarah Connor");
		sarahAsMap.put("gender", "MAN");
		sarahAsMap.put("_class",
				"org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$Person");

		kyleAsMap = Document.create();
		kyleAsMap.put("id", "kyle");
		kyleAsMap.put("gender", "MAN");
		kyleAsMap.put("name", "Kyle Reese");

		locationAsMap = Document.create();
		locationAsMap.put("lat", 34.118347D);
		locationAsMap.put("lon", -118.3026284D);

		gratiotAveAsMap = Document.create();
		gratiotAveAsMap.put("city", "Los Angeles");
		gratiotAveAsMap.put("street", "2800 East Observatory Road");
		gratiotAveAsMap.put("location", locationAsMap);

		bigBunsCafeAsMap = Document.create();
		bigBunsCafeAsMap.put("name", "Big Buns Cafe");
		bigBunsCafeAsMap.put("city", "Los Angeles");
		bigBunsCafeAsMap.put("street", "15 South Fremont Avenue");
		bigBunsCafeAsMap.put("location", new LinkedHashMap<>());
		((HashMap<String, Object>) bigBunsCafeAsMap.get("location")).put("lat", 34.0945637D);
		((HashMap<String, Object>) bigBunsCafeAsMap.get("location")).put("lon", -118.1545845D);
		bigBunsCafeAsMap.put("_class",
				"org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$Place");

		gunAsMap = Document.create();
		gunAsMap.put("label", "Glock 19");
		gunAsMap.put("shotsPerMagazine", 33);
		gunAsMap.put("_class", Gun.class.getName());

		grenadeAsMap = Document.create();
		grenadeAsMap.put("label", "40 mm");
		grenadeAsMap.put("_class", Grenade.class.getName());

		rifleAsMap = Document.create();
		rifleAsMap.put("label", "AR-18 Assault Rifle");
		rifleAsMap.put("weight", 3.17D);
		rifleAsMap.put("maxShotsPerMagazine", 40);
		rifleAsMap.put("_class", "rifle");

		shotGunAsMap = Document.create();
		shotGunAsMap.put("model", "Ithaca 37 Pump Shotgun");
		shotGunAsMap.put("_class", ShotGun.class.getName());

		notificationAsMap = Document.create();
		notificationAsMap.put("id", 1L);
		notificationAsMap.put("fromEmail", "from@email.com");
		notificationAsMap.put("toEmail", "to@email.com");
		Map<String, Object> data = new HashMap<>();
		data.put("documentType", "abc");
		data.put("content", null);
		notificationAsMap.put("params", data);
		notificationAsMap.put("_class",
				"org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$Notification");
	}

	private Map<String, Object> writeToMap(Object source) {

		Document sink = Document.create();
		mappingElasticsearchConverter.write(source, sink);
		return sink;
	}

	@Test
	public void shouldFailToInitializeGivenMappingContextIsNull() {

		assertThatThrownBy(() -> new MappingElasticsearchConverter(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldReturnMappingContextWithWhichItWasInitialized() {

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);

		assertThat(converter.getMappingContext()).isNotNull();
		assertThat(converter.getMappingContext()).isSameAs(mappingContext);
	}

	@Test
	public void shouldReturnDefaultConversionService() {

		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());

		ConversionService conversionService = converter.getConversionService();

		assertThat(conversionService).isNotNull();
	}

	@Test // DATAES-530
	public void shouldMapObjectToJsonString() throws JSONException {
		Car car = new Car();
		car.setModel(CAR_MODEL);
		car.setName(CAR_NAME);
		String jsonResult = mappingElasticsearchConverter.mapObject(car).toJson();

		assertEquals(jsonResult, JSON_STRING, false);
	}

	@Test // DATAES-530
	public void shouldReadJsonStringToObject() {

		Car result = mappingElasticsearchConverter.read(Car.class, Document.parse(JSON_STRING));

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(CAR_NAME);
		assertThat(result.getModel()).isEqualTo(CAR_MODEL);
	}

	@Test // DATAES-530
	public void shouldMapGeoPointElasticsearchNames() throws JSONException {
		double lon = 5;
		double lat = 48;
		Point point = new Point(lon, lat);
		// ES has Strings in "lat,lon", but has arrays as [lon,lat]!!
		String pointAsString = lat + "," + lon;
		double[] pointAsArray = { lon, lat };

		String expected = """
				{
				  "pointA": {
				    "lon": 5.0,
				    "lat": 48.0
				  },
				  "pointB": {
				    "lon": 5.0,
				    "lat": 48.0
				  },
				  "pointC": "48.0,5.0",
				  "pointD": [
				    5.0,
				    48.0
				  ]
				}
				"""; //

		GeoEntity geoEntity = new GeoEntity();
		geoEntity.setPointA(point);
		geoEntity.setPointB(GeoPoint.fromPoint(point));
		geoEntity.setPointC(pointAsString);
		geoEntity.setPointD(pointAsArray);
		String jsonResult = mappingElasticsearchConverter.mapObject(geoEntity).toJson();

		assertEquals(expected, jsonResult, false);
	}

	@Test // DATAES-530
	public void ignoresReadOnlyProperties() {

		Sample sample = new Sample();
		sample.setReadOnly("readOnly");
		sample.setProperty("property");
		sample.setJavaTransientProperty("javaTransient");
		sample.setAnnotatedTransientProperty("transient");

		String result = mappingElasticsearchConverter.mapObject(sample).toJson();

		assertThat(result).contains("\"property\"");
		assertThat(result).contains("\"javaTransient\"");

		assertThat(result).doesNotContain("readOnly");
		assertThat(result).doesNotContain("annotatedTransientProperty");
	}

	@Test // DATAES-530
	public void writesNestedEntity() {

		Person person = new Person();
		person.birthDate = LocalDate.now();
		person.gender = Gender.MAN;
		person.address = observatoryRoad;

		Map<String, Object> sink = writeToMap(person);

		assertThat(sink.get("address")).isEqualTo(gratiotAveAsMap);
	}

	@Test // DATAES-530
	public void writesConcreteList() {

		Person ginger = new Person();
		ginger.setId("ginger");
		ginger.setGender(Gender.MAN);

		sarahConnor.coWorkers = Arrays.asList(kyleReese, ginger);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat((List<Document>) target.get("coWorkers")).hasSize(2).contains(kyleAsMap);
	}

	@Test // DATAES-530
	public void writesInterfaceList() {

		Inventory gun = new Gun("Glock 19", 33);
		Inventory grenade = new Grenade("40 mm");

		sarahConnor.inventoryList = Arrays.asList(gun, grenade);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat((List<Document>) target.get("inventoryList")).containsExactly(gunAsMap, grenadeAsMap);
	}

	@Test // DATAES-530
	public void readTypeCorrectly() {

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target).isEqualTo(sarahConnor);
	}

	@Test // DATAES-530
	public void readListOfConcreteTypesCorrectly() {

		sarahAsMap.put("coWorkers", Collections.singletonList(kyleAsMap));

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getCoWorkers()).contains(kyleReese);
	}

	@Test // DATAES-530
	public void readListOfInterfacesTypesCorrectly() {

		sarahAsMap.put("inventoryList", Arrays.asList(gunAsMap, grenadeAsMap));

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getInventoryList()).containsExactly(gun, grenade);
	}

	@Test // DATAES-530
	public void writeMapOfConcreteType() {

		sarahConnor.shippingAddresses = new LinkedHashMap<>();
		sarahConnor.shippingAddresses.put("home", observatoryRoad);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat(target.get("shippingAddresses")).isInstanceOf(Map.class);
		assertThat(target.get("shippingAddresses")).isEqualTo(Collections.singletonMap("home", gratiotAveAsMap));
	}

	@Test // DATAES-530
	public void writeMapOfInterfaceType() {

		sarahConnor.inventoryMap = new LinkedHashMap<>();
		sarahConnor.inventoryMap.put("glock19", gun);
		sarahConnor.inventoryMap.put("40 mm grenade", grenade);

		Map<String, Object> target = writeToMap(sarahConnor);
		assertThat(target.get("inventoryMap")).isInstanceOf(Map.class);
		assertThat((Map<String, Document>) target.get("inventoryMap")).containsEntry("glock19", gunAsMap)
				.containsEntry("40 mm grenade", grenadeAsMap);
	}

	@Test // DATAES-530
	public void readConcreteMapCorrectly() {

		sarahAsMap.put("shippingAddresses", Collections.singletonMap("home", gratiotAveAsMap));

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getShippingAddresses()).hasSize(1).containsEntry("home", observatoryRoad);
	}

	@Test // DATAES-530
	public void readInterfaceMapCorrectly() {

		sarahAsMap.put("inventoryMap", Collections.singletonMap("glock19", gunAsMap));

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getInventoryMap()).hasSize(1).containsEntry("glock19", gun);
	}

	@Test // DATAES-530
	public void genericWriteList() {

		Skynet skynet = new Skynet();
		skynet.objectList = new ArrayList<>();
		skynet.objectList.add(t800);
		skynet.objectList.add(gun);

		Map<String, Object> target = writeToMap(skynet);

		assertThat((List<Object>) target.get("objectList")).containsExactly(t800AsMap, gunAsMap);
	}

	@Test // DATAES-530
	public void readGenericList() {

		Document source = Document.create();
		source.put("objectList", Arrays.asList(t800AsMap, gunAsMap));

		Skynet target = mappingElasticsearchConverter.read(Skynet.class, source);

		assertThat(target.getObjectList()).containsExactly(t800, gun);
	}

	@Test // DATAES-530
	public void genericWriteListWithList() {

		Skynet skynet = new Skynet();
		skynet.objectList = new ArrayList<>();
		skynet.objectList.add(Arrays.asList(t800, gun));

		Map<String, Object> target = writeToMap(skynet);

		assertThat((List<Object>) target.get("objectList")).containsExactly(Arrays.asList(t800AsMap, gunAsMap));
	}

	@Test // DATAES-530
	public void readGenericListList() {

		Document source = Document.create();
		source.put("objectList", Collections.singletonList(Arrays.asList(t800AsMap, gunAsMap)));

		Skynet target = mappingElasticsearchConverter.read(Skynet.class, source);

		assertThat(target.getObjectList()).containsExactly(Arrays.asList(t800, gun));
	}

	@Test // DATAES-530
	public void writeGenericMap() {

		Skynet skynet = new Skynet();
		skynet.objectMap = new LinkedHashMap<>();
		skynet.objectMap.put("gun", gun);
		skynet.objectMap.put("grenade", grenade);

		Map<String, Object> target = writeToMap(skynet);

		assertThat((Map<String, Object>) target.get("objectMap")).containsEntry("gun", gunAsMap).containsEntry("grenade",
				grenadeAsMap);
	}

	@Test // DATAES-530
	public void readGenericMap() {

		Document source = Document.create();
		source.put("objectMap", Collections.singletonMap("glock19", gunAsMap));

		Skynet target = mappingElasticsearchConverter.read(Skynet.class, source);

		assertThat(target.getObjectMap()).containsEntry("glock19", gun);
	}

	@Test // DATAES-530
	public void writeGenericMapMap() {

		Skynet skynet = new Skynet();
		skynet.objectMap = new LinkedHashMap<>();
		skynet.objectMap.put("inventory", Collections.singletonMap("glock19", gun));

		Map<String, Object> target = writeToMap(skynet);

		assertThat((Map<String, Object>) target.get("objectMap")).containsEntry("inventory",
				Collections.singletonMap("glock19", gunAsMap));
	}

	@Test // DATAES-530
	public void readGenericMapMap() {

		Document source = Document.create();
		source.put("objectMap", Collections.singletonMap("inventory", Collections.singletonMap("glock19", gunAsMap)));

		Skynet target = mappingElasticsearchConverter.read(Skynet.class, source);

		assertThat(target.getObjectMap()).containsEntry("inventory", Collections.singletonMap("glock19", gun));
	}

	@Test // DATAES-530
	public void readsNestedEntity() {

		sarahAsMap.put("address", gratiotAveAsMap);

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.getAddress()).isEqualTo(observatoryRoad);
	}

	@Test // DATAES-530
	public void readsNestedObjectEntity() {

		Document source = Document.create();
		source.put("object", t800AsMap);

		Skynet target = mappingElasticsearchConverter.read(Skynet.class, source);

		assertThat(target.getObject()).isEqualTo(t800);
	}

	@Test // DATAES-530
	public void writesAliased() {
		assertThat(writeToMap(rifle)).containsEntry("_class", "rifle").doesNotContainValue(Rifle.class.getName());
	}

	@Test // DATAES-530
	public void writesNestedAliased() {

		t800.inventoryList = Collections.singletonList(rifle);
		Map<String, Object> target = writeToMap(t800);

		assertThat((List<Document>) target.get("inventoryList")).contains(rifleAsMap);
	}

	@Test // DATAES-530
	public void readsAliased() {
		assertThat(mappingElasticsearchConverter.read(Inventory.class, rifleAsMap)).isEqualTo(rifle);
	}

	@Test // DATAES-530
	public void readsNestedAliased() {

		t800AsMap.put("inventoryList", Collections.singletonList(rifleAsMap));

		assertThat(mappingElasticsearchConverter.read(Person.class, t800AsMap).getInventoryList()).containsExactly(rifle);
	}

	@Test // DATAES-530
	public void appliesCustomConverterForWrite() {
		assertThat(writeToMap(shotGun)).isEqualTo(shotGunAsMap);
	}

	@Test // DATAES-530
	public void appliesCustomConverterForRead() {
		assertThat(mappingElasticsearchConverter.read(Inventory.class, shotGunAsMap)).isEqualTo(shotGun);
	}

	@Test // DATAES-530
	public void writeSubTypeCorrectly() {

		sarahConnor.address = bigBunsCafe;

		Map<String, Object> target = writeToMap(sarahConnor);

		assertThat(target.get("address")).isEqualTo(bigBunsCafeAsMap);
	}

	@Test // DATAES-530
	public void readSubTypeCorrectly() {

		sarahAsMap.put("address", bigBunsCafeAsMap);

		Person target = mappingElasticsearchConverter.read(Person.class, sarahAsMap);

		assertThat(target.address).isEqualTo(bigBunsCafe);
	}

	@Test // DATAES-716
	void shouldWriteLocalDate() throws JSONException {
		Person person = new Person();
		person.setId("4711");
		person.setFirstName("John");
		person.setLastName("Doe");
		person.birthDate = LocalDate.of(2000, 8, 22);
		person.gender = Gender.MAN;

		String expected = """
				{
				  "id": "4711",
				  "first-name": "John",
				  "last-name": "Doe",
				  "birth-date": "22.08.2000",
				  "gender": "MAN"
				}
				""";
		Document document = Document.create();
		mappingElasticsearchConverter.write(person, document);
		String json = document.toJson();

		assertEquals(expected, json, false);
	}

	@Test // DATAES-924
	@DisplayName("should write list of LocalDate")
	void shouldWriteListOfLocalDate() throws JSONException {

		LocalDatesEntity entity = new LocalDatesEntity();
		entity.setId("4711");
		entity.setDates(Arrays.asList(LocalDate.of(2020, 9, 15), LocalDate.of(2019, 5, 1)));
		String expected = """
				{
				  "id": "4711",
				  "dates": ["15.09.2020", "01.05.2019"]
				}
				"""; //

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, false);
	}

	@Test // DATAES-716
	void shouldReadLocalDate() {
		Document document = Document.create();
		document.put("id", "4711");
		document.put("first-name", "John");
		document.put("last-name", "Doe");
		document.put("birth-date", "22.08.2000");
		document.put("gender", "MAN");

		Person person = mappingElasticsearchConverter.read(Person.class, document);

		assertThat(person.getId()).isEqualTo("4711");
		assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(2000, 8, 22));
		assertThat(person.getGender()).isEqualTo(Gender.MAN);
	}

	@Test // DATAES-924
	@DisplayName("should read list of LocalDate")
	void shouldReadListOfLocalDate() {

		Document document = Document.create();
		document.put("id", "4711");
		document.put("dates", new String[] { "15.09.2020", "01.05.2019" });

		LocalDatesEntity entity = mappingElasticsearchConverter.read(LocalDatesEntity.class, document);

		assertThat(entity.getId()).isEqualTo("4711");
		assertThat(entity.getDates()).hasSize(2).containsExactly(LocalDate.of(2020, 9, 15), LocalDate.of(2019, 5, 1));
	}

	@Test // DATAES-763
	void writeEntityWithMapDataType() {

		Notification notification = new Notification();
		notification.setFromEmail("from@email.com");
		notification.setToEmail("to@email.com");
		Map<String, Object> data = new HashMap<>();
		data.put("documentType", "abc");
		data.put("content", null);
		notification.params = data;
		notification.id = 1L;

		Document document = Document.create();
		mappingElasticsearchConverter.write(notification, document);
		assertThat(document).isEqualTo(notificationAsMap);
	}

	@Test // DATAES-763
	void readEntityWithMapDataType() {

		Document document = Document.create();
		document.put("id", 1L);
		document.put("fromEmail", "from@email.com");
		document.put("toEmail", "to@email.com");
		Map<String, Object> data = new HashMap<>();
		data.put("documentType", "abc");
		data.put("content", null);
		document.put("params", data);

		Notification notification = mappingElasticsearchConverter.read(Notification.class, document);
		assertThat(notification.params.get("documentType")).isEqualTo("abc");
		assertThat(notification.params.get("content")).isNull();
	}

	@Test // DATAES-795
	void readGenericMapWithSimpleTypes() {
		Map<String, Object> mapWithSimpleValues = new HashMap<>();
		mapWithSimpleValues.put("int", 1);
		mapWithSimpleValues.put("string", "string");
		mapWithSimpleValues.put("boolean", true);

		Document document = Document.create();
		document.put("schemaLessObject", mapWithSimpleValues);

		SchemaLessObjectWrapper wrapper = mappingElasticsearchConverter.read(SchemaLessObjectWrapper.class, document);
		assertThat(wrapper.getSchemaLessObject()).isEqualTo(mapWithSimpleValues);
	}

	@Test // DATAES-797
	void readGenericListWithMaps() {
		Map<String, Object> simpleMap = new HashMap<>();
		simpleMap.put("int", 1);

		List<Map<String, Object>> listWithSimpleMap = new ArrayList<>();
		listWithSimpleMap.add(simpleMap);

		Map<String, List<Map<String, Object>>> mapWithSimpleList = new HashMap<>();
		mapWithSimpleList.put("someKey", listWithSimpleMap);

		Document document = Document.create();
		document.put("schemaLessObject", mapWithSimpleList);

		SchemaLessObjectWrapper wrapper = mappingElasticsearchConverter.read(SchemaLessObjectWrapper.class, document);
		assertThat(wrapper.getSchemaLessObject()).isEqualTo(mapWithSimpleList);
	}

	@Test // DATAES-799
	void shouldNotWriteSeqNoPrimaryTermProperty() {
		EntityWithSeqNoPrimaryTerm entity = new EntityWithSeqNoPrimaryTerm();
		entity.seqNoPrimaryTerm = new SeqNoPrimaryTerm(1L, 2L);
		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertThat(document).doesNotContainKey("seqNoPrimaryTerm");
	}

	@Test // DATAES-799
	void shouldNotReadSeqNoPrimaryTermProperty() {
		Document document = Document.create().append("seqNoPrimaryTerm", emptyMap());

		EntityWithSeqNoPrimaryTerm entity = mappingElasticsearchConverter.read(EntityWithSeqNoPrimaryTerm.class, document);

		assertThat(entity.seqNoPrimaryTerm).isNull();
	}

	@Test // DATAES-845
	void shouldWriteCollectionsWithNullValues() throws JSONException {
		EntityWithListProperty entity = new EntityWithListProperty();
		entity.setId("42");
		entity.setValues(Arrays.asList(null, "two", null, "four"));

		String expected = '{' + //
				"  \"id\": \"42\"," + //
				"  \"values\": [null, \"two\", null, \"four\"]" + //
				'}';
		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, false);
	}

	@Test // DATAES-857
	void shouldWriteEntityWithListOfGeoPoints() throws JSONException {

		GeoPointListEntity entity = new GeoPointListEntity();
		entity.setId("42");
		List<GeoPoint> locations = Arrays.asList(new GeoPoint(12.34, 23.45), new GeoPoint(34.56, 45.67));
		entity.setLocations(locations);

		String expected = """
				{
				  "id": "42",
				  "locations": [
				    {
				      "lat": 12.34,
				      "lon": 23.45
				    },
				    {
				      "lat": 34.56,
				      "lon": 45.67
				    }
				  ]
				}"""; //
		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, false);
	}

	@Test // DATAES-857
	void shouldReadEntityWithListOfGeoPoints() {

		String json = """
				{
				  "id": "42",
				  "locations": [
				    {
				      "lat": 12.34,
				      "lon": 23.45
				    },
				    {
				      "lat": 34.56,
				      "lon": 45.67
				    }
				  ]
				}"""; //

		Document document = Document.parse(json);

		GeoPointListEntity entity = mappingElasticsearchConverter.read(GeoPointListEntity.class, document);

		assertThat(entity.id).isEqualTo("42");
		assertThat(entity.locations).containsExactly(new GeoPoint(12.34, 23.45), new GeoPoint(34.56, 45.67));
	}

	@Test // DATAES-865
	void shouldWriteEntityWithMapAsObject() throws JSONException {

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("foo", "bar");

		EntityWithObject entity = new EntityWithObject();
		entity.setId("42");
		entity.setContent(map);

		String expected = """
				{
				  "id": "42",
				  "content": {
				    "foo": "bar"
				  }
				}
				"""; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertEquals(expected, document.toJson(), false);
	}

	@Test // DATAES-920
	@DisplayName("should write null value if configured")
	void shouldWriteNullValueIfConfigured() throws JSONException {

		EntityWithNullField entity = new EntityWithNullField();
		entity.setId("42");

		String expected = """
				{
				  "id": "42",
				  "saved": null
				}
				"""; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertEquals(expected, document.toJson(), false);
	}

	@Test // #2627
	@DisplayName("should write Map containing collection containing map")
	void shouldWriteMapContainingCollectionContainingMap() throws JSONException {

		class EntityWithMapCollectionMap {
			Map<String, Object> map;
		}
		class InnerEntity {
			String prop1;

			String prop2;

			public InnerEntity() {}

			public InnerEntity(String prop1, String prop2) {
				this.prop1 = prop1;
				this.prop2 = prop2;
			}

		}

		var entity = new EntityWithMapCollectionMap();
		entity.map = Collections.singletonMap("collection",
				Collections.singletonList(Collections.singletonMap("destination", new InnerEntity("prop1", "prop2"))));

		var expected = """
				{
					"_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$1EntityWithMapCollectionMap",
					"map": {
						"collection": [
							{
								"destination": {
									"_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$1InnerEntity",
									"prop1": "prop1",
									"prop2": "prop2"
								}
							}
						]
					}
				}
				""";

		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertEquals(expected, document.toJson(), false);
	}

	@Nested
	class RangeTests {

		static final String JSON = """
				{
				  "_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$RangeTests$RangeEntity",
				  "integerRange": {
				    "gt": "1",
				    "lt": "10"
				  },
				  "floatRange": {
				    "gte": "1.2",
				    "lte": "2.5"
				  },
				  "longRange": {
				    "gt": "2",
				    "lte": "5"
				  },
				  "doubleRange": {
				    "gte": "3.2",
				    "lt": "7.4"
				  },
				  "dateRange": {
				    "gte": "1970-01-01T00:00:00.000Z",
				    "lte": "1970-01-01T01:00:00.000Z"
				  },
				  "localDateRange": {
				    "gte": "2021-07-06"
				  },
				  "localTimeRange": {
				    "gte": "00:30:00.000",
				    "lt": "02:30:00.000"
				  },
				  "localDateTimeRange": {
				    "gt": "2021-01-01T00:30:00.000",
				    "lt": "2021-01-01T02:30:00.000"
				  },
				  "offsetTimeRange": {
				    "gte": "00:30:00.000+02:00",
				    "lt": "02:30:00.000+02:00"
				  },
				  "zonedDateTimeRange": {
				    "gte": "2021-01-01T00:30:00.000+02:00",
				    "lte": "2021-01-01T00:30:00.000+02:00"
				  },
				  "nullRange": null,
				  "integerRangeList": [
				  	{
				  	  "gte": "2",
				  	  "lte": "5"
				  	}
				  ]
				}
				""";

		@Test
		public void shouldReadRanges() throws JSONException {

			Document source = Document.parse(JSON);

			RangeEntity entity = mappingElasticsearchConverter.read(RangeEntity.class, source);

			assertThat(entity) //
					.isNotNull() //
					.satisfies(e -> {
						assertThat(e.getIntegerRange()).isEqualTo(Range.open(1, 10));
						assertThat(e.getFloatRange()).isEqualTo(Range.closed(1.2f, 2.5f));
						assertThat(e.getLongRange()).isEqualTo(Range.leftOpen(2l, 5l));
						assertThat(e.getDoubleRange()).isEqualTo(Range.rightOpen(3.2d, 7.4d));
						assertThat(e.getDateRange()).isEqualTo(Range.closed(new Date(0), new Date(60 * 60 * 1000)));
						assertThat(e.getLocalDateRange())
								.isEqualTo(Range.rightUnbounded(Range.Bound.inclusive(LocalDate.of(2021, 7, 6))));
						assertThat(e.getLocalTimeRange()).isEqualTo(Range.rightOpen(LocalTime.of(0, 30), LocalTime.of(2, 30)));
						assertThat(e.getLocalDateTimeRange())
								.isEqualTo(Range.open(LocalDateTime.of(2021, 1, 1, 0, 30), LocalDateTime.of(2021, 1, 1, 2, 30)));
						assertThat(e.getOffsetTimeRange())
								.isEqualTo(Range.rightOpen(OffsetTime.of(LocalTime.of(0, 30), ZoneOffset.ofHours(2)),
										OffsetTime.of(LocalTime.of(2, 30), ZoneOffset.ofHours(2))));
						assertThat(e.getZonedDateTimeRange()).isEqualTo(
								Range.just(ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(0, 30), ZoneOffset.ofHours(2))));
						assertThat(e.getNullRange()).isNull();
						assertThat(e.getIntegerRangeList()).containsExactly(Range.closed(2, 5));
					});
		}

		@Test
		public void shouldWriteRanges() throws JSONException {

			Document source = Document.parse(JSON);
			RangeEntity entity = new RangeEntity();
			entity.setIntegerRange(Range.open(1, 10));
			entity.setFloatRange(Range.closed(1.2f, 2.5f));
			entity.setLongRange(Range.leftOpen(2l, 5l));
			entity.setDoubleRange(Range.rightOpen(3.2d, 7.4d));
			entity.setDateRange(Range.closed(new Date(0), new Date(60 * 60 * 1000)));
			entity.setLocalDateRange(Range.rightUnbounded(Range.Bound.inclusive(LocalDate.of(2021, 7, 6))));
			entity.setLocalTimeRange(Range.rightOpen(LocalTime.of(0, 30), LocalTime.of(2, 30)));
			entity
					.setLocalDateTimeRange(Range.open(LocalDateTime.of(2021, 1, 1, 0, 30), LocalDateTime.of(2021, 1, 1, 2, 30)));
			entity.setOffsetTimeRange(Range.rightOpen(OffsetTime.of(LocalTime.of(0, 30), ZoneOffset.ofHours(2)),
					OffsetTime.of(LocalTime.of(2, 30), ZoneOffset.ofHours(2))));
			entity.setZonedDateTimeRange(
					Range.just(ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(0, 30), ZoneOffset.ofHours(2))));
			entity.setNullRange(null);
			entity.setIntegerRangeList(List.of(Range.closed(2, 5)));
			Document document = mappingElasticsearchConverter.mapObject(entity);

			assertThat(document).isEqualTo(source);
		}

		@org.springframework.data.elasticsearch.annotations.Document(indexName = "test-index-range-entity-mapper")
		class RangeEntity {

			@Id private String id;
			@Field(type = FieldType.Integer_Range) private Range<Integer> integerRange;
			@Field(type = FieldType.Float_Range) private Range<Float> floatRange;
			@Field(type = FieldType.Long_Range) private Range<Long> longRange;
			@Field(type = FieldType.Double_Range) private Range<Double> doubleRange;
			@Field(type = FieldType.Date_Range) private Range<Date> dateRange;
			@Field(type = FieldType.Date_Range, format = DateFormat.year_month_day) private Range<LocalDate> localDateRange;
			@Field(type = FieldType.Date_Range,
					format = DateFormat.hour_minute_second_millis) private Range<LocalTime> localTimeRange;
			@Field(type = FieldType.Date_Range,
					format = DateFormat.date_hour_minute_second_millis) private Range<LocalDateTime> localDateTimeRange;
			@Field(type = FieldType.Date_Range, format = DateFormat.time) private Range<OffsetTime> offsetTimeRange;
			@Field(type = FieldType.Date_Range) private Range<ZonedDateTime> zonedDateTimeRange;
			@Field(type = FieldType.Date_Range, storeNullValue = true) private Range<ZonedDateTime> nullRange;

			@Field(type = FieldType.Integer_Range) private List<Range<Integer>> integerRangeList;

			public String getId() {
				return id;
			}

			public Range<Integer> getIntegerRange() {
				return integerRange;
			}

			public Range<Float> getFloatRange() {
				return floatRange;
			}

			public Range<Long> getLongRange() {
				return longRange;
			}

			public Range<Double> getDoubleRange() {
				return doubleRange;
			}

			public Range<Date> getDateRange() {
				return dateRange;
			}

			public Range<LocalDate> getLocalDateRange() {
				return localDateRange;
			}

			public Range<LocalTime> getLocalTimeRange() {
				return localTimeRange;
			}

			public Range<LocalDateTime> getLocalDateTimeRange() {
				return localDateTimeRange;
			}

			public Range<OffsetTime> getOffsetTimeRange() {
				return offsetTimeRange;
			}

			public Range<ZonedDateTime> getZonedDateTimeRange() {
				return zonedDateTimeRange;
			}

			public Range<ZonedDateTime> getNullRange() {
				return nullRange;
			}

			public void setId(String id) {
				this.id = id;
			}

			public void setIntegerRange(Range<Integer> integerRange) {
				this.integerRange = integerRange;
			}

			public void setFloatRange(Range<Float> floatRange) {
				this.floatRange = floatRange;
			}

			public void setLongRange(Range<Long> longRange) {
				this.longRange = longRange;
			}

			public void setDoubleRange(Range<Double> doubleRange) {
				this.doubleRange = doubleRange;
			}

			public void setDateRange(Range<Date> dateRange) {
				this.dateRange = dateRange;
			}

			public void setLocalDateRange(Range<LocalDate> localDateRange) {
				this.localDateRange = localDateRange;
			}

			public void setLocalTimeRange(Range<LocalTime> localTimeRange) {
				this.localTimeRange = localTimeRange;
			}

			public void setLocalDateTimeRange(Range<LocalDateTime> localDateTimeRange) {
				this.localDateTimeRange = localDateTimeRange;
			}

			public void setOffsetTimeRange(Range<OffsetTime> offsetTimeRange) {
				this.offsetTimeRange = offsetTimeRange;
			}

			public void setZonedDateTimeRange(Range<ZonedDateTime> zonedDateTimeRange) {
				this.zonedDateTimeRange = zonedDateTimeRange;
			}

			public void setNullRange(Range<ZonedDateTime> nullRange) {
				this.nullRange = nullRange;
			}

			public List<Range<Integer>> getIntegerRangeList() {
				return integerRangeList;
			}

			public void setIntegerRangeList(List<Range<Integer>> integerRangeList) {
				this.integerRangeList = integerRangeList;
			}

		}
	}

	@Nested
	class GeoJsonUnitTests {

		private GeoJsonEntity entity;

		@BeforeEach
		void setup() {
			GeoJsonMultiLineString multiLineString = GeoJsonMultiLineString.of(Arrays.asList( //
					GeoJsonLineString.of(new Point(12, 34), new Point(56, 78)), //
					GeoJsonLineString.of(new Point(90, 12), new Point(34, 56)) //
			));
			GeoJsonPolygon geoJsonPolygon = GeoJsonPolygon //
					.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)) //
					.withInnerRing(new Point(21, 43), new Point(65, 87), new Point(9, 21), new Point(21, 43));
			GeoJsonMultiPolygon geoJsonMultiPolygon = GeoJsonMultiPolygon.of(
					Arrays.asList(GeoJsonPolygon.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)),
							GeoJsonPolygon.of(new Point(21, 43), new Point(65, 87), new Point(9, 21), new Point(21, 43))));
			GeoJsonPoint geoJsonPoint = GeoJsonPoint.of(12, 34);
			GeoJsonGeometryCollection geoJsonGeometryCollection = GeoJsonGeometryCollection
					.of(Arrays.asList(GeoJsonPoint.of(12, 34), GeoJsonPolygon
							.of(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12), new Point(12, 34)))));

			entity = new GeoJsonEntity();
			entity.setId("42");
			entity.setPoint1(GeoJsonPoint.of(12, 34));
			entity.setPoint2(GeoJsonPoint.of(56, 78));
			entity.setMultiPoint1(GeoJsonMultiPoint.of(new Point(12, 34), new Point(56, 78), new Point(90, 12)));
			entity.setMultiPoint2(GeoJsonMultiPoint.of(new Point(90, 12), new Point(56, 78), new Point(12, 34)));
			entity.setLineString1(GeoJsonLineString.of(new Point(12, 34), new Point(56, 78), new Point(90, 12)));
			entity.setLineString2(GeoJsonLineString.of(new Point(90, 12), new Point(56, 78), new Point(12, 34)));
			entity.setMultiLineString1(multiLineString);
			entity.setMultiLineString2(multiLineString);
			entity.setPolygon1(geoJsonPolygon);
			entity.setPolygon2(geoJsonPolygon);
			entity.setMultiPolygon1(geoJsonMultiPolygon);
			entity.setMultiPolygon2(geoJsonMultiPolygon);
			entity.setGeometryCollection1(geoJsonGeometryCollection);
			entity.setGeometryCollection2(geoJsonGeometryCollection);
		}

		@Test // DATAES-930
		@DisplayName("should write GeoJson properties")
		void shouldWriteGeoJsonProperties() throws JSONException {

			String json = """
					{
					  "id": "42",
					  "point1": {
					    "type": "Point",
					    "coordinates": [12.0, 34.0]
					  },
					  "point2": {
					    "type": "Point",
					    "coordinates": [56.0, 78.0]
					  },
					  "multiPoint1": {
					    "type": "MultiPoint",
					    "coordinates": [
					      [12.0, 34.0],
					      [56.0, 78.0],
					      [90.0, 12.0]
					    ]
					  },
					  "multiPoint2": {
					    "type": "MultiPoint",
					    "coordinates": [
					      [90.0, 12.0],
					      [56.0, 78.0],
					      [12.0, 34.0]
					    ]
					  },
					  "lineString1": {
					    "type": "LineString",
					    "coordinates": [
					      [12.0, 34.0],
					      [56.0, 78.0],
					      [90.0, 12.0]
					    ]
					  },
					  "lineString2": {
					    "type": "LineString",
					    "coordinates": [
					      [90.0, 12.0],
					      [56.0, 78.0],
					      [12.0, 34.0]
					    ]
					  },
					  "multiLineString1":{
					    "type": "MultiLineString",
					    "coordinates": [
					      [[12.0, 34.0], [56.0, 78.0]],
					      [[90.0, 12.0], [34.0, 56.0]]
					    ]
					  },
					  "multiLineString2":{
					    "type": "MultiLineString",
					    "coordinates": [
					      [[12.0, 34.0], [56.0, 78.0]],
					      [[90.0, 12.0], [34.0, 56.0]]
					    ]
					  },
					  "polygon1":{
					    "type": "Polygon",
					    "coordinates": [
					      [[12.0, 34.0],[56.0, 78.0],[90.0, 12.0],[12.0, 34.0]],
					      [[21.0, 43.0],[65.0, 87.0],[9.0, 21.0],[21.0, 43.0]]
					    ]
					  },
					  "polygon2":{
					    "type": "Polygon",
					    "coordinates": [
					      [[12.0, 34.0],[56.0, 78.0],[90.0, 12.0],[12.0, 34.0]],
					      [[21.0, 43.0],[65.0, 87.0],[9.0, 21.0],[21.0, 43.0]]
					    ]
					  },
					  "multiPolygon1":{
					    "type": "MultiPolygon",
					    "coordinates": [
					      [[[12.0, 34.0],[56.0, 78.0],[90.0, 12.0],[12.0, 34.0]]],
					      [[[21.0, 43.0],[65.0, 87.0],[9.0, 21.0],[21.0, 43.0]]]
					    ]
					  },
					  "multiPolygon2":{
					    "type": "MultiPolygon",
					    "coordinates": [
					      [[[12.0, 34.0],[56.0, 78.0],[90.0, 12.0],[12.0, 34.0]]],
					      [[[21.0, 43.0],[65.0, 87.0],[9.0, 21.0],[21.0, 43.0]]]
					    ]
					  },
					  "geometryCollection1": {
					    "type": "GeometryCollection",
					    "geometries": [
					      {
					        "type": "Point",
					        "coordinates": [12.0, 34.0]
					      },
					      {
					        "type": "Polygon",
					        "coordinates": [
					          [[12.0, 34.0], [56.0, 78.0], [90.0, 12.0], [12.0, 34.0]]
					        ]
					      }
					    ]
					  },
					  "geometryCollection2": {
					    "type": "GeometryCollection",
					    "geometries": [
					      {
					        "type": "Point",
					        "coordinates": [12.0, 34.0]
					      },
					      {
					        "type": "Polygon",
					        "coordinates": [
					          [[12.0, 34.0], [56.0, 78.0], [90.0, 12.0], [12.0, 34.0]]
					        ]
					      }
					    ]
					  }
					}
					"""; //

			Document document = Document.create();

			mappingElasticsearchConverter.write(entity, document);

			assertEquals(json, document.toJson(), false);
		}

		@Test // DATAES-930
		@DisplayName("should read GeoJson properties")
		void shouldReadGeoJsonProperties() {

			// make sure we can read int values as well
			String json = """
					{
					  "id": "42",
					  "point1": {
					    "type": "Point",
					    "coordinates": [12, 34]
					  },
					  "point2": {
					    "type": "Point",
					    "coordinates": [56, 78]
					  },
					  "multiPoint1": {
					    "type": "MultiPoint",
					    "coordinates": [
					      [12.0, 34],
					      [56, 78.0],
					      [90, 12.0]
					    ]
					  },
					  "multiPoint2": {
					    "type": "MultiPoint",
					    "coordinates": [
					      [90, 12.0],
					      [56, 78.0],
					      [12.0, 34]
					    ]
					  },
					  "lineString1": {
					    "type": "LineString",
					    "coordinates": [
					      [12.0, 34],
					      [56, 78.0],
					      [90, 12.0]
					    ]
					  },
					  "lineString2": {
					    "type": "LineString",
					    "coordinates": [
					      [90, 12.0],
					      [56, 78.0],
					      [12.0, 34]
					    ]
					  },
					  "multiLineString1":{
					    "type": "MultiLineString",
					    "coordinates": [
					      [[12, 34.0], [56, 78.0]],
					      [[90.0, 12], [34.0, 56]]
					    ]
					  },
					  "multiLineString2":{
					    "type": "MultiLineString",
					    "coordinates": [
					      [[12.0, 34], [56.0, 78]],
					      [[90, 12.0], [34, 56.0]]
					    ]
					  },
					  "polygon1":{
					    "type": "Polygon",
					    "coordinates": [
					      [[12, 34.0],[56.0, 78],[90, 12.0],[12.0, 34]],
					      [[21.0, 43],[65, 87.0],[9.0, 21],[21, 43.0]]
					    ]
					  },
					  "polygon2":{
					    "type": "Polygon",
					    "coordinates": [
					      [[12, 34.0],[56.0, 78],[90, 12.0],[12.0, 34]],
					      [[21.0, 43],[65, 87.0],[9.0, 21],[21, 43.0]]
					    ]
					  },
					  "multiPolygon1":{
					    "type": "MultiPolygon",
					    "coordinates": [
					      [[[12, 34.0],[56.0, 78],[90, 12.0],[12.0, 34]]],
					      [[[21.0, 43],[65, 87.0],[9.0, 21],[21, 43.0]]]
					    ]
					  },
					  "multiPolygon2":{
					    "type": "MultiPolygon",
					    "coordinates": [
					      [[[12, 34.0],[56.0, 78],[90, 12.0],[12.0, 34]]],
					      [[[21.0, 43],[65, 87.0],[9.0, 21],[21, 43.0]]]
					    ]
					  },
					  "geometryCollection1": {
					    "type": "GeometryCollection",
					    "geometries": [
					      {
					        "type": "Point",
					        "coordinates": [12, 34.0]
					      },
					      {
					        "type": "Polygon",
					        "coordinates": [
					          [[12.0, 34], [56, 78.0], [90.0, 12], [12, 34.0]]
					        ]
					      }
					    ]
					  },
					  "geometryCollection2": {
					    "type": "GeometryCollection",
					    "geometries": [
					      {
					        "type": "Point",
					        "coordinates": [12, 34.0]
					      },
					      {
					        "type": "Polygon",
					        "coordinates": [
					          [[12.0, 34], [56, 78.0], [90.0, 12], [12, 34.0]]
					        ]
					      }
					    ]
					  }
					}
					"""; //

			GeoJsonEntity mapped = mappingElasticsearchConverter.read(GeoJsonEntity.class, Document.parse(json));

			assertThat(entity).isEqualTo(mapped);
		}

	}

	@Test // #1454
	@DisplayName("should write type hints if configured")
	void shouldWriteTypeHintsIfConfigured() throws JSONException {

		((SimpleElasticsearchMappingContext) mappingElasticsearchConverter.getMappingContext()).setWriteTypeHints(true);
		PersonWithCars person = new PersonWithCars();
		person.setId("42");
		person.setName("Smith");
		Car car1 = new Car();
		car1.setModel("Ford Mustang");
		Car car2 = new ElectricCar();
		car2.setModel("Porsche Taycan");
		person.setCars(Arrays.asList(car1, car2));

		String expected = """
				{
				  "_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$PersonWithCars",
				  "id": "42",
				  "name": "Smith",
				  "cars": [
				    {
				      "model": "Ford Mustang"
				    },
				    {
				      "_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$ElectricCar",
				      "model": "Porsche Taycan"
				    }
				  ]
				}
				"""; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(person, document);

		assertEquals(expected, document.toJson(), true);
	}

	@Test // #1454
	@DisplayName("should not write type hints if configured")
	void shouldNotWriteTypeHintsIfNotConfigured() throws JSONException {

		((SimpleElasticsearchMappingContext) mappingElasticsearchConverter.getMappingContext()).setWriteTypeHints(false);
		PersonWithCars person = new PersonWithCars();
		person.setId("42");
		person.setName("Smith");
		Car car1 = new Car();
		car1.setModel("Ford Mustang");
		Car car2 = new ElectricCar();
		car2.setModel("Porsche Taycan");
		person.setCars(Arrays.asList(car1, car2));

		String expected = """
				{
				  "id": "42",
				  "name": "Smith",
				  "cars": [
				    {
				      "model": "Ford Mustang"
				    },
				    {
				      "model": "Porsche Taycan"
				    }
				  ]
				}
				"""; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(person, document);

		assertEquals(expected, document.toJson(), true);
	}

	@Test // #1945
	@DisplayName("should write using ValueConverters")
	void shouldWriteUsingValueConverters() throws JSONException {

		EntityWithCustomValueConverters entity = new EntityWithCustomValueConverters();
		entity.setId("42");
		entity.setFieldWithClassBasedConverter("classbased");
		entity.setFieldWithEnumBasedConverter("enumbased");
		entity.setDontConvert("Monty Python's Flying Circus");

		String expected = """
				{
				  "id": "42",
				  "fieldWithClassBasedConverter": "desabssalc",
				  "fieldWithEnumBasedConverter": "desabmune",
				  "dontConvert": "Monty Python's Flying Circus"
				}
				"""; //

		Document document = Document.create();

		mappingElasticsearchConverter.write(entity, document);

		assertEquals(expected, document.toJson(), false);
	}

	@Test // #1945
	@DisplayName("should read using ValueConverters")
	void shouldReadUsingValueConverters() throws JSONException {

		String json = """
				{
				  "id": "42",
				  "fieldWithClassBasedConverter": "desabssalc",
				  "fieldWithEnumBasedConverter": "desabmune",
				  "dontConvert": "Monty Python's Flying Circus"
				}
				"""; //

		Document source = Document.parse(json);

		EntityWithCustomValueConverters entity = mappingElasticsearchConverter.read(EntityWithCustomValueConverters.class,
				source);

		assertThat(entity.getId()).isEqualTo("42");
		assertThat(entity.getFieldWithClassBasedConverter()).isEqualTo("classbased");
		assertThat(entity.getFieldWithEnumBasedConverter()).isEqualTo("enumbased");
		assertThat(entity.getDontConvert()).isEqualTo("Monty Python's Flying Circus");
	}

	@Test // #2080
	@DisplayName("should not try to call property converter on updating criteria exists")
	void shouldNotTryToCallPropertyConverterOnUpdatingCriteriaExists() {

		// don't care if the query makes no sense, we just add all criteria without values
		Query query = new CriteriaQuery(Criteria.where("fieldWithClassBasedConverter").exists().empty().notEmpty());

		mappingElasticsearchConverter.updateQuery(query, EntityWithCustomValueConverters.class);
	}

	@Test // #2280
	@DisplayName("should read a single String into a List property")
	void shouldReadASingleStringIntoAListProperty() {

		@Language("JSON")
		var json = """
				{
					"stringList": "foo"
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a List property")
	void shouldReadAStringArrayIntoAListProperty() {

		@Language("JSON")
		var json = """
				{
					"stringList": ["foo", "bar"]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single String into a Set property")
	void shouldReadASingleStringIntoASetProperty() {

		@Language("JSON")
		var json = """
				{
					"stringSet": "foo"
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a Set property")
	void shouldReadAStringArrayIntoASetProperty() {

		@Language("JSON")
		var json = """
				{
					"stringSet": ["foo", "bar"]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single object into a List property")
	void shouldReadASingleObjectIntoAListProperty() {

		@Language("JSON")
		var json = """
				{
					"childrenList": {
						"name": "child"
					}
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(1);
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a List property")
	void shouldReadAnObjectArrayIntoAListProperty() {

		@Language("JSON")
		var json = """
				{
					"childrenList": [
						{
							"name": "child1"
						},
						{
							"name": "child2"
						}
					]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(2);
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child1");
		assertThat(entity.getChildrenList().get(1).getName()).isEqualTo("child2");
	}

	@Test // #2280
	@DisplayName("should read a single object into a Set property")
	void shouldReadASingleObjectIntoASetProperty() {

		@Language("JSON")
		var json = """
				{
					"childrenSet": {
						"name": "child"
					}
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(1);
		assertThat(entity.getChildrenSet().iterator().next().getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a Set property")
	void shouldReadAnObjectArrayIntoASetProperty() {

		@Language("JSON")
		var json = """
				{
					"childrenSet": [
						{
							"name": "child1"
						},
						{
							"name": "child2"
						}
					]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(EntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(2);
		List<String> names = entity.getChildrenSet().stream().map(EntityWithCollections.Child::getName)
				.collect(Collectors.toList());
		assertThat(names).containsExactlyInAnyOrder("child1", "child2");
	}

	@Test // #2280
	@DisplayName("should read a single String into a List property immutable")
	void shouldReadASingleStringIntoAListPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"stringList": "foo"
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a List property immutable")
	void shouldReadAStringArrayIntoAListPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"stringList": ["foo", "bar"]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringList()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single String into a Set property immutable")
	void shouldReadASingleStringIntoASetPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"stringSet": "foo"
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo");
	}

	@Test // #2280
	@DisplayName("should read a String array into a Set property immutable")
	void shouldReadAStringArrayIntoASetPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"stringSet": ["foo", "bar"]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getStringSet()).containsExactly("foo", "bar");
	}

	@Test // #2280
	@DisplayName("should read a single object into a List property immutable")
	void shouldReadASingleObjectIntoAListPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"childrenList": {
						"name": "child"
					}
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(1);
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a List property immutable")
	void shouldReadAnObjectArrayIntoAListPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"childrenList": [
						{
							"name": "child1"
						},
						{
							"name": "child2"
						}
					]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenList()).hasSize(2);
		assertThat(entity.getChildrenList().get(0).getName()).isEqualTo("child1");
		assertThat(entity.getChildrenList().get(1).getName()).isEqualTo("child2");
	}

	@Test // #2280
	@DisplayName("should read a single object into a Set property immutable")
	void shouldReadASingleObjectIntoASetPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"childrenSet": {
						"name": "child"
					}
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(1);
		assertThat(entity.getChildrenSet().iterator().next().getName()).isEqualTo("child");
	}

	@Test // #2280
	@DisplayName("should read an object array into a Set property immutable")
	void shouldReadAnObjectArrayIntoASetPropertyImmutable() {

		@Language("JSON")
		var json = """
				{
					"childrenSet": [
						{
							"name": "child1"
						},
						{
							"name": "child2"
						}
					]
				}
				""";
		Document source = Document.parse(json);

		var entity = mappingElasticsearchConverter.read(ImmutableEntityWithCollections.class, source);

		assertThat(entity.getChildrenSet()).hasSize(2);
		List<String> names = entity.getChildrenSet().stream().map(ImmutableEntityWithCollections.Child::getName)
				.collect(Collectors.toList());
		assertThat(names).containsExactlyInAnyOrder("child1", "child2");
	}

	@Test // #2364
	@DisplayName("should not write id property to document source if configured so")
	void shouldNotWriteIdPropertyToDocumentSourceIfConfiguredSo() throws JSONException {

		@Language("JSON")
		var expected = """
				{
					"_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$DontWriteIdToSourceEntity",
					"text": "some text"
				}
				""";
		var entity = new DontWriteIdToSourceEntity();
		entity.setId("42");
		entity.setText("some text");

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, true);
	}

	@Test // #2364
	@DisplayName("should not write version property to document source if configured so")
	void shouldNotWriteVersionPropertyToDocumentSourceIfConfiguredSo() throws JSONException {

		@Language("JSON")
		var expected = """
				{
					"_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$DontWriteVersionToSourceEntity",
					"id": "42",
					"text": "some text"
				}
				""";
		var entity = new DontWriteVersionToSourceEntity();
		entity.setId("42");
		entity.setVersion(7L);
		entity.setText("some text");

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, true);
	}

	@Test // #2290
	@DisplayName("should respect field setting for empty properties")
	void shouldRespectFieldSettingForEmptyProperties() throws JSONException {
		@Language("JSON")
		var expected = """
				{
					"_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$EntityWithPropertiesThatMightBeEmpty",
					"id": "42",
					"stringToWriteWhenEmpty": "",
					"listToWriteWhenEmpty": [],
					"mapToWriteWhenEmpty": {}
				}
				""";
		var entity = new EntityWithPropertiesThatMightBeEmpty();
		entity.setId("42");
		entity.setStringToWriteWhenEmpty("");
		entity.setStringToNotWriteWhenEmpty("");
		entity.setListToWriteWhenEmpty(emptyList());
		entity.setListToNotWriteWhenEmpty(emptyList());
		entity.setMapToWriteWhenEmpty(emptyMap());
		entity.setMapToNotWriteWhenEmpty(emptyMap());

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, true);
	}

	@Test // #2502
	@DisplayName("should write entity with dotted field name")
	void shouldWriteEntityWithDottedFieldName() throws JSONException {

		@Language("JSON")
		var expected = """
					{
						"_class": "org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverterUnitTests$FieldNameDotsEntity",
						"id": "42",
						"dotted.field": "dotted field"
					}
				""";
		var entity = new FieldNameDotsEntity();
		entity.setId("42");
		entity.setDottedField("dotted field");

		Document document = Document.create();
		mappingElasticsearchConverter.write(entity, document);
		String json = document.toJson();

		assertEquals(expected, json, true);
	}

	@Test // #2502
	@DisplayName("should read entity with dotted field name")
	void shouldReadEntityWithDottedFieldName() {

		@Language("JSON")
		String json = """
				{
				  "id": "42",
				  "dotted.field": "dotted field"
				}""";

		Document document = Document.parse(json);

		FieldNameDotsEntity entity = mappingElasticsearchConverter.read(FieldNameDotsEntity.class, document);

		assertThat(entity.id).isEqualTo("42");
		assertThat(entity.getDottedField()).isEqualTo("dotted field");
	}

	@Test // #1784
	@DisplayName("should map property path to field names")
	void shouldMapPropertyPathToFieldNames() {

		var propertyPath = "level1Entries.level2Entries.keyWord";
		ElasticsearchPersistentEntity<?> persistentEntity = mappingElasticsearchConverter.getMappingContext()
				.getPersistentEntity(NestedEntity.class);
		var mappedNames = mappingElasticsearchConverter.updateFieldNames(propertyPath, persistentEntity);

		assertThat(mappedNames).isEqualTo("level-one.level-two.key-word");
	}

	@Test // #2879
	@DisplayName("should throw MappingConversionException with document id on reading error")
	void shouldThrowMappingConversionExceptionWithDocumentIdOnReadingError() {

		@Language("JSON")
		String json = """
				{
				  "birth-date": "this-is-not-a-local-date"
				}""";

		Document document = Document.parse(json);
		document.setId("42");

		assertThatThrownBy(() -> {
			mappingElasticsearchConverter.read(Person.class, document);
		}).isInstanceOf(MappingConversionException.class).hasFieldOrPropertyWithValue("documentId", "42")
				.hasCauseInstanceOf(ConversionException.class);
	}

	// region entities
	public static class Sample {
		@Nullable public @ReadOnlyProperty String readOnly;
		@Nullable public @Transient String annotatedTransientProperty;
		@Nullable public transient String javaTransientProperty;
		@Nullable public String property;

		@Nullable
		public String getReadOnly() {
			return readOnly;
		}

		public void setReadOnly(@Nullable String readOnly) {
			this.readOnly = readOnly;
		}

		@Nullable
		public String getAnnotatedTransientProperty() {
			return annotatedTransientProperty;
		}

		public void setAnnotatedTransientProperty(@Nullable String annotatedTransientProperty) {
			this.annotatedTransientProperty = annotatedTransientProperty;
		}

		@Nullable
		public String getJavaTransientProperty() {
			return javaTransientProperty;
		}

		public void setJavaTransientProperty(@Nullable String javaTransientProperty) {
			this.javaTransientProperty = javaTransientProperty;
		}

		@Nullable
		public String getProperty() {
			return property;
		}

		public void setProperty(@Nullable String property) {
			this.property = property;
		}
	}

	static class Person {
		@Nullable
		@Id String id;
		@Nullable String name;
		@Nullable
		@Field(name = "first-name") String firstName;
		@Nullable
		@Field(name = "last-name") String lastName;
		@Nullable
		@Field(name = "birth-date", type = FieldType.Date, format = {}, pattern = "dd.MM.uuuu") LocalDate birthDate;
		@Nullable Gender gender;
		@Nullable Address address;
		@Nullable List<Person> coWorkers;
		@Nullable List<Inventory> inventoryList;
		@Nullable Map<String, Address> shippingAddresses;
		@Nullable Map<String, Inventory> inventoryMap;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public LocalDate getBirthDate() {
			return birthDate;
		}

		public void setBirthDate(@Nullable LocalDate birthDate) {
			this.birthDate = birthDate;
		}

		@Nullable
		public Gender getGender() {
			return gender;
		}

		public void setGender(@Nullable Gender gender) {
			this.gender = gender;
		}

		@Nullable
		public Address getAddress() {
			return address;
		}

		public void setAddress(@Nullable Address address) {
			this.address = address;
		}

		@Nullable
		public List<Person> getCoWorkers() {
			return coWorkers;
		}

		public void setCoWorkers(@Nullable List<Person> coWorkers) {
			this.coWorkers = coWorkers;
		}

		@Nullable
		public List<Inventory> getInventoryList() {
			return inventoryList;
		}

		public void setInventoryList(@Nullable List<Inventory> inventoryList) {
			this.inventoryList = inventoryList;
		}

		@Nullable
		public Map<String, Address> getShippingAddresses() {
			return shippingAddresses;
		}

		public void setShippingAddresses(@Nullable Map<String, Address> shippingAddresses) {
			this.shippingAddresses = shippingAddresses;
		}

		@Nullable
		public Map<String, Inventory> getInventoryMap() {
			return inventoryMap;
		}

		public void setInventoryMap(@Nullable Map<String, Inventory> inventoryMap) {
			this.inventoryMap = inventoryMap;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Person person = (Person) o;

			if (!Objects.equals(id, person.id))
				return false;
			if (!Objects.equals(name, person.name))
				return false;
			if (!Objects.equals(firstName, person.firstName))
				return false;
			if (!Objects.equals(lastName, person.lastName))
				return false;
			if (!Objects.equals(birthDate, person.birthDate))
				return false;
			if (gender != person.gender)
				return false;
			if (!Objects.equals(address, person.address))
				return false;
			if (!Objects.equals(coWorkers, person.coWorkers))
				return false;
			if (!Objects.equals(inventoryList, person.inventoryList))
				return false;
			if (!Objects.equals(shippingAddresses, person.shippingAddresses))
				return false;
			return Objects.equals(inventoryMap, person.inventoryMap);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
			result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
			result = 31 * result + (birthDate != null ? birthDate.hashCode() : 0);
			result = 31 * result + (gender != null ? gender.hashCode() : 0);
			result = 31 * result + (address != null ? address.hashCode() : 0);
			result = 31 * result + (coWorkers != null ? coWorkers.hashCode() : 0);
			result = 31 * result + (inventoryList != null ? inventoryList.hashCode() : 0);
			result = 31 * result + (shippingAddresses != null ? shippingAddresses.hashCode() : 0);
			result = 31 * result + (inventoryMap != null ? inventoryMap.hashCode() : 0);
			return result;
		}
	}

	static class LocalDatesEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(name = "dates", type = FieldType.Date, format = {}, pattern = "dd.MM.uuuu") private List<LocalDate> dates;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public List<LocalDate> getDates() {
			return dates;
		}

		public void setDates(@Nullable List<LocalDate> dates) {
			this.dates = dates;
		}
	}

	enum Gender {

		MAN("1"), MACHINE("0");

		final String theValue;

		Gender(String theValue) {
			this.theValue = theValue;
		}

		public String getTheValue() {
			return theValue;
		}
	}

	interface Inventory {
		String label();
	}

	record Gun(String label, int shotsPerMagazine) implements Inventory {
	}

	record Grenade(String label) implements Inventory {
	}

	@TypeAlias("rifle")
	record Rifle(String label, double weight, int maxShotsPerMagazine) implements Inventory {
	}

	record ShotGun(String label) implements Inventory {
	}

	static class Address {
		@Nullable private Point location;
		@Nullable private String street;
		@Nullable private String city;

		@Nullable
		public Point getLocation() {
			return location;
		}

		public void setLocation(@Nullable Point location) {
			this.location = location;
		}

		@Nullable
		public String getStreet() {
			return street;
		}

		public void setStreet(@Nullable String street) {
			this.street = street;
		}

		@Nullable
		public String getCity() {
			return city;
		}

		public void setCity(@Nullable String city) {
			this.city = city;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Address address))
				return false;

			if (!Objects.equals(location, address.location))
				return false;
			if (!Objects.equals(street, address.street))
				return false;
			return Objects.equals(city, address.city);
		}

		@Override
		public int hashCode() {
			int result = location != null ? location.hashCode() : 0;
			result = 31 * result + (street != null ? street.hashCode() : 0);
			result = 31 * result + (city != null ? city.hashCode() : 0);
			return result;
		}
	}

	static class Place extends Address {
		@Nullable private String name;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Place place))
				return false;

			return Objects.equals(name, place.name);
		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}
	}

	static class Skynet {
		@Nullable private Object object;
		@Nullable private List<Object> objectList;
		@Nullable private Map<String, Object> objectMap;

		@Nullable
		public Object getObject() {
			return object;
		}

		public void setObject(@Nullable Object object) {
			this.object = object;
		}

		@Nullable
		public List<Object> getObjectList() {
			return objectList;
		}

		public void setObjectList(@Nullable List<Object> objectList) {
			this.objectList = objectList;
		}

		@Nullable
		public Map<String, Object> getObjectMap() {
			return objectMap;
		}

		public void setObjectMap(@Nullable Map<String, Object> objectMap) {
			this.objectMap = objectMap;
		}
	}

	static class Notification {
		@Nullable private Long id;
		@Nullable private String fromEmail;
		@Nullable private String toEmail;
		@Nullable private Map<String, Object> params;

		@Nullable
		public Long getId() {
			return id;
		}

		public void setId(@Nullable Long id) {
			this.id = id;
		}

		@Nullable
		public String getFromEmail() {
			return fromEmail;
		}

		public void setFromEmail(@Nullable String fromEmail) {
			this.fromEmail = fromEmail;
		}

		@Nullable
		public String getToEmail() {
			return toEmail;
		}

		public void setToEmail(@Nullable String toEmail) {
			this.toEmail = toEmail;
		}

		@Nullable
		public Map<String, Object> getParams() {
			return params;
		}

		public void setParams(@Nullable Map<String, Object> params) {
			this.params = params;
		}
	}

	@WritingConverter
	static class ShotGunToMapConverter implements Converter<ShotGun, Map<String, Object>> {

		@Override
		public Map<String, Object> convert(ShotGun source) {

			LinkedHashMap<String, Object> target = new LinkedHashMap<>();
			target.put("model", source.label());
			target.put("_class", ShotGun.class.getName());
			return target;
		}
	}

	@ReadingConverter
	static class MapToShotGunConverter implements Converter<Map<String, Object>, ShotGun> {

		@Override
		public ShotGun convert(Map<String, Object> source) {
			return new ShotGun(source.get("model").toString());
		}
	}

	static class Car {
		@Nullable private String name;
		@Nullable private String model;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getModel() {
			return model;
		}

		public void setModel(@Nullable String model) {
			this.model = model;
		}
	}

	@org.springframework.data.elasticsearch.annotations.Document(indexName = "test-index-geo-core-entity-mapper")
	static class GeoEntity {
		@Nullable
		@Id private String id;
		// geo shape - Spring Data
		@Nullable private Box box;
		@Nullable private Circle circle;
		@Nullable private Polygon polygon;
		// geo point - Custom implementation + Spring Data
		@Nullable
		@GeoPointField private Point pointA;
		@Nullable private GeoPoint pointB;
		@Nullable
		@GeoPointField private String pointC;
		@Nullable
		@GeoPointField private double[] pointD;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Box getBox() {
			return box;
		}

		public void setBox(@Nullable Box box) {
			this.box = box;
		}

		@Nullable
		public Circle getCircle() {
			return circle;
		}

		public void setCircle(@Nullable Circle circle) {
			this.circle = circle;
		}

		@Nullable
		public Polygon getPolygon() {
			return polygon;
		}

		public void setPolygon(@Nullable Polygon polygon) {
			this.polygon = polygon;
		}

		@Nullable
		public Point getPointA() {
			return pointA;
		}

		public void setPointA(@Nullable Point pointA) {
			this.pointA = pointA;
		}

		@Nullable
		public GeoPoint getPointB() {
			return pointB;
		}

		public void setPointB(@Nullable GeoPoint pointB) {
			this.pointB = pointB;
		}

		@Nullable
		public String getPointC() {
			return pointC;
		}

		public void setPointC(@Nullable String pointC) {
			this.pointC = pointC;
		}

		@Nullable
		public double[] getPointD() {
			return pointD;
		}

		public void setPointD(@Nullable double[] pointD) {
			this.pointD = pointD;
		}
	}

	static class SchemaLessObjectWrapper {
		@Nullable private Map<String, Object> schemaLessObject;

		@Nullable
		public Map<String, Object> getSchemaLessObject() {
			return schemaLessObject;
		}

		public void setSchemaLessObject(@Nullable Map<String, Object> schemaLessObject) {
			this.schemaLessObject = schemaLessObject;
		}
	}

	@org.springframework.data.elasticsearch.annotations.Document(
			indexName = "test-index-entity-with-seq-no-primary-term-mapper")
	static class EntityWithSeqNoPrimaryTerm {
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(@Nullable SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}

	static class EntityWithListProperty {
		@Nullable
		@Id private String id;
		@Nullable private List<String> values;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public List<String> getValues() {
			return values;
		}

		public void setValues(@Nullable List<String> values) {
			this.values = values;
		}
	}

	static class GeoPointListEntity {
		@Nullable private @Id String id;
		@Nullable private List<GeoPoint> locations;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public List<GeoPoint> getLocations() {
			return locations;
		}

		public void setLocations(@Nullable List<GeoPoint> locations) {
			this.locations = locations;
		}
	}

	static class EntityWithObject {
		@Nullable
		@Id private String id;
		@Nullable private Object content;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Object getContent() {
			return content;
		}

		public void setContent(@Nullable Object content) {
			this.content = content;
		}
	}

	static class EntityWithNullField {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String notSaved;
		@Nullable
		@Field(type = FieldType.Text, storeNullValue = true) private String saved;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getNotSaved() {
			return notSaved;
		}

		public void setNotSaved(@Nullable String notSaved) {
			this.notSaved = notSaved;
		}

		@Nullable
		public String getSaved() {
			return saved;
		}

		public void setSaved(@Nullable String saved) {
			this.saved = saved;
		}
	}

	private static class ElectricCar extends Car {}

	private static class PersonWithCars {
		@Id
		@Nullable String id;
		@Field(type = FieldType.Text)
		@Nullable private String name;
		@Field(type = FieldType.Nested)
		@Nullable private List<? extends Car> cars;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public List<? extends Car> getCars() {
			return cars;
		}

		public void setCars(@Nullable List<Car> cars) {
			this.cars = cars;
		}
	}

	private static class EntityWithCustomValueConverters {
		@Nullable
		@Id private String id;
		@Nullable
		@ValueConverter(ClassBasedValueConverter.class) private String fieldWithClassBasedConverter;
		@Nullable
		@ValueConverter(EnumBasedValueConverter.class) private String fieldWithEnumBasedConverter;
		@Nullable private String dontConvert;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getFieldWithClassBasedConverter() {
			return fieldWithClassBasedConverter;
		}

		public void setFieldWithClassBasedConverter(@Nullable String fieldWithClassBasedConverter) {
			this.fieldWithClassBasedConverter = fieldWithClassBasedConverter;
		}

		@Nullable
		public String getFieldWithEnumBasedConverter() {
			return fieldWithEnumBasedConverter;
		}

		public void setFieldWithEnumBasedConverter(@Nullable String fieldWithEnumBasedConverter) {
			this.fieldWithEnumBasedConverter = fieldWithEnumBasedConverter;
		}

		@Nullable
		public String getDontConvert() {
			return dontConvert;
		}

		public void setDontConvert(@Nullable String dontConvert) {
			this.dontConvert = dontConvert;
		}
	}

	private static class ClassBasedValueConverter implements PropertyValueConverter {

		@Override
		public Object write(Object value) {
			return reverse(value);
		}

		@Override
		public Object read(Object value) {
			return reverse(value);
		}
	}

	private enum EnumBasedValueConverter implements PropertyValueConverter {
		INSTANCE;

		@Override
		public Object write(Object value) {
			return reverse(value);
		}

		@Override
		public Object read(Object value) {
			return reverse(value);
		}
	}

	private static class EntityWithCollections {
		@Field(type = FieldType.Keyword)
		@Nullable private List<String> stringList;

		@Field(type = FieldType.Keyword)
		@Nullable private Set<String> stringSet;

		@Field(type = FieldType.Object)
		@Nullable private List<Child> childrenList;

		@Field(type = FieldType.Object)
		@Nullable private Set<Child> childrenSet;

		@Nullable
		public List<String> getStringList() {
			return stringList;
		}

		public void setStringList(@Nullable List<String> stringList) {
			this.stringList = stringList;
		}

		@Nullable
		public Set<String> getStringSet() {
			return stringSet;
		}

		public void setStringSet(@Nullable Set<String> stringSet) {
			this.stringSet = stringSet;
		}

		@Nullable
		public List<Child> getChildrenList() {
			return childrenList;
		}

		public void setChildrenList(@Nullable List<Child> childrenList) {
			this.childrenList = childrenList;
		}

		@Nullable
		public Set<Child> getChildrenSet() {
			return childrenSet;
		}

		public void setChildrenSet(@Nullable Set<Child> childrenSet) {
			this.childrenSet = childrenSet;
		}

		public static class Child {

			@Field(type = FieldType.Keyword)
			@Nullable private String name;

			@Nullable
			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}
		}
	}

	private static final class ImmutableEntityWithCollections {
		@Field(type = FieldType.Keyword)
		@Nullable private final List<String> stringList;

		@Field(type = FieldType.Keyword)
		@Nullable private final Set<String> stringSet;

		@Field(type = FieldType.Object)
		@Nullable private final List<Child> childrenList;

		@Field(type = FieldType.Object)
		@Nullable private final Set<Child> childrenSet;

		public ImmutableEntityWithCollections(@Nullable List<String> stringList, @Nullable Set<String> stringSet,
				@Nullable List<Child> childrenList, @Nullable Set<Child> childrenSet) {
			this.stringList = stringList;
			this.stringSet = stringSet;
			this.childrenList = childrenList;
			this.childrenSet = childrenSet;
		}

		@Nullable
		public List<String> getStringList() {
			return stringList;
		}

		@Nullable
		public Set<String> getStringSet() {
			return stringSet;
		}

		@Nullable
		public List<Child> getChildrenList() {
			return childrenList;
		}

		@Nullable
		public Set<Child> getChildrenSet() {
			return childrenSet;
		}

		public static class Child {

			@Field(type = FieldType.Keyword)
			@Nullable private final String name;

			public Child(@Nullable String name) {
				this.name = name;
			}

			@Nullable
			public String getName() {
				return name;
			}
		}
	}

	@org.springframework.data.elasticsearch.annotations.Document(indexName = "doesnt-matter", storeIdInSource = false)
	static class DontWriteIdToSourceEntity {
		@Nullable private String id;
		@Nullable
		@Field(type = FieldType.Text) private String text;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}

	@org.springframework.data.elasticsearch.annotations.Document(indexName = "doesnt-matter",
			storeVersionInSource = false)
	static class DontWriteVersionToSourceEntity {
		@Nullable private String id;
		@Version
		@Nullable private Long version;
		@Nullable
		@Field(type = FieldType.Text) private String text;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Long getVersion() {
			return version;
		}

		public void setVersion(@Nullable Long version) {
			this.version = version;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}

	static class EntityWithPropertiesThatMightBeEmpty {
		@Nullable private String id;

		@Field(type = FieldType.Text)
		@Nullable private String stringToWriteWhenEmpty;

		@Field(type = FieldType.Text, storeEmptyValue = false)
		@Nullable private String stringToNotWriteWhenEmpty;

		@Field(type = FieldType.Nested)
		@Nullable private List<String> listToWriteWhenEmpty;

		@Field(type = FieldType.Nested, storeEmptyValue = false)
		@Nullable private List<String> listToNotWriteWhenEmpty;

		@Field(type = FieldType.Nested)
		@Nullable private Map<String, String> mapToWriteWhenEmpty;

		@Field(type = FieldType.Nested, storeEmptyValue = false)
		@Nullable private Map<String, String> mapToNotWriteWhenEmpty;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getStringToWriteWhenEmpty() {
			return stringToWriteWhenEmpty;
		}

		public void setStringToWriteWhenEmpty(@Nullable String stringToWriteWhenEmpty) {
			this.stringToWriteWhenEmpty = stringToWriteWhenEmpty;
		}

		@Nullable
		public String getStringToNotWriteWhenEmpty() {
			return stringToNotWriteWhenEmpty;
		}

		public void setStringToNotWriteWhenEmpty(@Nullable String stringToNotWriteWhenEmpty) {
			this.stringToNotWriteWhenEmpty = stringToNotWriteWhenEmpty;
		}

		@Nullable
		public List<String> getListToWriteWhenEmpty() {
			return listToWriteWhenEmpty;
		}

		public void setListToWriteWhenEmpty(@Nullable List<String> listToWriteWhenEmpty) {
			this.listToWriteWhenEmpty = listToWriteWhenEmpty;
		}

		@Nullable
		public List<String> getListToNotWriteWhenEmpty() {
			return listToNotWriteWhenEmpty;
		}

		public void setListToNotWriteWhenEmpty(@Nullable List<String> listToNotWriteWhenEmpty) {
			this.listToNotWriteWhenEmpty = listToNotWriteWhenEmpty;
		}

		@Nullable
		public Map<String, String> getMapToWriteWhenEmpty() {
			return mapToWriteWhenEmpty;
		}

		public void setMapToWriteWhenEmpty(@Nullable Map<String, String> mapToWriteWhenEmpty) {
			this.mapToWriteWhenEmpty = mapToWriteWhenEmpty;
		}

		@Nullable
		public Map<String, String> getMapToNotWriteWhenEmpty() {
			return mapToNotWriteWhenEmpty;
		}

		public void setMapToNotWriteWhenEmpty(@Nullable Map<String, String> mapToNotWriteWhenEmpty) {
			this.mapToNotWriteWhenEmpty = mapToNotWriteWhenEmpty;
		}
	}

	static class FieldNameDotsEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(name = "dotted.field", type = FieldType.Text) private String dottedField;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getDottedField() {
			return dottedField;
		}

		public void setDottedField(@Nullable String dottedField) {
			this.dottedField = dottedField;
		}
	}

	static class NestedEntity {
		@Id
		@Nullable private String id;

		@Field(type = FieldType.Nested, name = "level-one") private List<Level1> level1Entries;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		public List<Level1> getLevel1Entries() {
			return level1Entries;
		}

		public void setLevel1Entries(List<Level1> level1Entries) {
			this.level1Entries = level1Entries;
		}

		static class Level1 {
			@Field(type = FieldType.Nested, name = "level-two") private List<Level2> level2Entries;

			public List<Level2> getLevel2Entries() {
				return level2Entries;
			}

			public void setLevel2Entries(List<Level2> level2Entries) {
				this.level2Entries = level2Entries;
			}
		}

		static class Level2 {
			@Field(type = FieldType.Keyword, name = "key-word") private String keyWord;

			public String getKeyWord() {
				return keyWord;
			}

			public void setKeyWord(String keyWord) {
				this.keyWord = keyWord;
			}
		}
	}

	// endregion

	private static String reverse(Object o) {

		Assert.notNull(o, "o must not be null");

		return new StringBuilder().append(o).reverse().toString();
	}
}
