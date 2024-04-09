/*
 * Copyright 2020-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import static org.skyscreamer.jsonassert.JSONAssert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchPartQuery;
import org.springframework.data.elasticsearch.repository.query.ElasticsearchQueryMethod;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

/**
 * Tests for {@link ElasticsearchPartQuery}. The tests make sure that queries are built according to the method naming.
 * Classes implementing this abstract class are in the packages of their request factories and converters as these are
 * kept package private.
 *
 * @author Peter-Josef Meisch
 * @author Haibo Liu
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringIntegrationTest
public abstract class ElasticsearchPartQueryIntegrationTests {

	public static final String BOOK_TITLE = "Title";
	public static final int BOOK_PRICE = 42;

	@Autowired protected ElasticsearchOperations operations;

	@Test
	void findByName() throws NoSuchMethodException, JSONException {
		String methodName = "findByName";
		Class<?>[] parameterClasses = new Class[] { String.class };
		Object[] parameters = new Object[] { BOOK_TITLE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_TITLE + "\", \"fields\" : [\"name\"]}}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameNot() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameNot";
		Class<?>[] parameterClasses = new Class[] { String.class };
		Object[] parameters = new Object[] { BOOK_TITLE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must_not\" : [" + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_TITLE + "\", \"fields\" : [\"name\"]}}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameLike() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameLike";
		Class<?>[] parameterClasses = new Class[] { String.class };
		Object[] parameters = new Object[] { BOOK_TITLE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_TITLE
				+ "*\", \"fields\" : [\"name\"], \"analyze_wildcard\": true }}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameStartingWith() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameStartingWith";
		Class<?>[] parameterClasses = new Class[] { String.class };
		Object[] parameters = new Object[] { BOOK_TITLE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_TITLE
				+ "*\", \"fields\" : [\"name\"], \"analyze_wildcard\": true }}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameEndingWith() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameEndingWith";
		Class<?>[] parameterClasses = new Class[] { String.class };
		Object[] parameters = new Object[] { BOOK_TITLE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"*" + BOOK_TITLE
				+ "\", \"fields\" : [\"name\"], \"analyze_wildcard\": true }}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameContaining() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameContaining";
		Class<?>[] parameterClasses = new Class[] { String.class };
		Object[] parameters = new Object[] { BOOK_TITLE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"*" + BOOK_TITLE
				+ "*\", \"fields\" : [\"name\"], \"analyze_wildcard\": true }}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameIn() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameIn";
		Class<?>[] parameterClasses = new Class[] { Collection.class };
		List<String> names = new ArrayList<>();
		names.add(BOOK_TITLE);
		names.add(BOOK_TITLE + "2");
		Object[] parameters = new Object[] { names };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "query_string": {
				            "query": "\\"Title\\" \\"Title2\\"",
				            "fields": [
				              "name"
				            ]
				          }
				        }
				      ]
				    }
				  }
				}
				"""; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameNotIn() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameNotIn";
		Class<?>[] parameterClasses = new Class[] { Collection.class };
		List<String> names = new ArrayList<>();
		names.add(BOOK_TITLE);
		names.add(BOOK_TITLE + "2");
		Object[] parameters = new Object[] { names };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "query_string": {
				            "query": "NOT(\\"Title\\" \\"Title2\\")",
				            "fields": [
				              "name"
				            ]
				          }
				        }
				      ]
				    }
				  }
				}
				"""; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByPrice() throws NoSuchMethodException, JSONException {
		String methodName = "findByPrice";
		Class<?>[] parameterClasses = new Class[] { Integer.class };
		Object[] parameters = new Object[] { 42 };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_PRICE + "\", \"fields\" : [\"price\"]}}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameAndPrice() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameAndPrice";
		Class<?>[] parameterClasses = new Class[] { String.class, Integer.class };
		Object[] parameters = new Object[] { BOOK_TITLE, BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_TITLE + "\", \"fields\" : [\"name\"]}}," + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_PRICE + "\", \"fields\" : [\"price\"]}}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByNameOrPrice() throws NoSuchMethodException, JSONException {
		String methodName = "findByNameOrPrice";
		Class<?>[] parameterClasses = new Class[] { String.class, Integer.class };
		Object[] parameters = new Object[] { BOOK_TITLE, BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"should\" : [" + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_TITLE + "\", \"fields\" : [\"name\"]}}," + //
				"      {\"query_string\" : {\"query\" : \"" + BOOK_PRICE + "\", \"fields\" : [\"price\"]}}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByPriceBetween() throws NoSuchMethodException, JSONException {
		String methodName = "findByPriceBetween";
		Class<?>[] parameterClasses = new Class[] { Integer.class, Integer.class };
		Object[] parameters = new Object[] { BOOK_PRICE - 10, BOOK_PRICE + 10 };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expectedERHLC = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"range\" : {\"price\" : {\"from\" : 32, \"to\" : 52, \"include_lower\" : true, \"include_upper\" : true } } }"
				+ //
				"    ]" + //
				"  }" + //
				"}}"; //

		String expectedELC = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "range": {
				            "price": {
				              "gte": 32,
				              "lte": 52
				            }
				          }
				        }
				      ]
				    }
				  }
				}
				"""; //

		assertAny( //
				() -> assertEquals(expectedERHLC, query, false) //
				, () -> assertEquals(expectedELC, query, false) //
		);
	}

	@Test
	void findByPriceLessThan() throws NoSuchMethodException, JSONException {
		String methodName = "findByPriceLessThan";
		Class<?>[] parameterClasses = new Class[] { Integer.class };
		Object[] parameters = new Object[] { BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expectedERHLC = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"range\" : {\"price\" : {\"from\" : null, \"to\" : 42, \"include_lower\" : true, \"include_upper\" : false } } }"
				+ //
				"    ]" + //
				"  }" + //
				"}}"; //

		String expectedELC = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "range": {
				            "price": {
				              "lt": 42
				            }
				          }
				        }
				      ]
				    }
				  }
				}"""; //

		assertAny( //
				() -> assertEquals(expectedERHLC, query, false) //
				, () -> assertEquals(expectedELC, query, false) //
		);
	}

	@Test
	void findByPriceLessThanEqual() throws NoSuchMethodException, JSONException {
		String methodName = "findByPriceLessThanEqual";
		Class<?>[] parameterClasses = new Class[] { Integer.class };
		Object[] parameters = new Object[] { BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expectedERHLC = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"range\" : {\"price\" : {\"from\" : null, \"to\" : 42, \"include_lower\" : true, \"include_upper\" : true } } }"
				+ //
				"    ]" + //
				"  }" + //
				"}}"; //

		String expectedELC = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "range": {
				            "price": {
				              "lte": 42
				            }
				          }
				        }
				      ]
				    }
				  }
				}"""; //

		assertAny( //
				() -> assertEquals(expectedERHLC, query, false) //
				, () -> assertEquals(expectedELC, query, false) //
		);
	}

	@Test
	void findByPriceGreaterThan() throws NoSuchMethodException, JSONException {
		String methodName = "findByPriceGreaterThan";
		Class<?>[] parameterClasses = new Class[] { Integer.class };
		Object[] parameters = new Object[] { BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expectedERHLC = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"range\" : {\"price\" : {\"from\" : 42, \"to\" : null, \"include_lower\" : false, \"include_upper\" : true } } }"
				+ //
				"    ]" + //
				"  }" + //
				"}}"; //

		String expectedELC = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "range": {
				            "price": {
				              "gt": 42
				            }
				          }
				        }
				      ]
				    }
				  }
				}"""; //

		assertAny( //
				() -> assertEquals(expectedERHLC, query, false) //
				, () -> assertEquals(expectedELC, query, false) //
		);
	}

	@Test
	void findByPriceGreaterThanEqual() throws NoSuchMethodException, JSONException {
		String methodName = "findByPriceGreaterThanEqual";
		Class<?>[] parameterClasses = new Class[] { Integer.class };
		Object[] parameters = new Object[] { BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expectedERHLC = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"range\" : {\"price\" : {\"from\" : 42, \"to\" : null, \"include_lower\" : true, \"include_upper\" : true } } }"
				+ //
				"    ]" + //
				"  }" + //
				"}}"; //

		String expectedELC = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "range": {
				            "price": {
				              "gte": 42
				            }
				          }
				        }
				      ]
				    }
				  }
				}"""; //

		assertAny( //
				() -> assertEquals(expectedERHLC, query, false) //
				, () -> assertEquals(expectedELC, query, false) //
		);
	}

	@Test
	void findByPriceBefore() throws NoSuchMethodException, JSONException {
		String methodName = "findByPriceBefore";
		Class<?>[] parameterClasses = new Class[] { Integer.class };
		Object[] parameters = new Object[] { BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expectedERHLC = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"range\" : {\"price\" : {\"from\" : null, \"to\" : 42, \"include_lower\" : true, \"include_upper\" : true } } }"
				+ //
				"    ]" + //
				"  }" + //
				"}}"; //

		String expectedELC = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "range": {
				            "price": {
				              "lte": 42
				            }
				          }
				        }
				      ]
				    }
				  }
				}"""; //

		assertAny( //
				() -> assertEquals(expectedERHLC, query, false) //
				, () -> assertEquals(expectedELC, query, false) //
		);
	}

	@Test
	void findByPriceAfter() throws NoSuchMethodException, JSONException {
		String methodName = "findByPriceAfter";
		Class<?>[] parameterClasses = new Class[] { Integer.class };
		Object[] parameters = new Object[] { BOOK_PRICE };

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expectedERHLC = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"range\" : {\"price\" : {\"from\" : 42, \"to\" : null, \"include_lower\" : true, \"include_upper\" : true } } }"
				+ //
				"    ]" + //
				"  }" + //
				"}}"; //

		String expectedELC = """
				{
				  "query": {
				    "bool": {
				      "must": [
				        {
				          "range": {
				            "price": {
				              "gte": 42
				            }
				          }
				        }
				      ]
				    }
				  }
				}"""; //

		assertAny( //
				() -> assertEquals(expectedERHLC, query, false) //
				, () -> assertEquals(expectedELC, query, false) //
		);
	}

	@Test
	void findByAvailableTrue() throws NoSuchMethodException, JSONException {
		String methodName = "findByAvailableTrue";
		Class<?>[] parameterClasses = new Class[] {};
		Object[] parameters = new Object[] {};

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"true\", \"fields\" : [\"available\"]}}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByAvailableFalse() throws NoSuchMethodException, JSONException {
		String methodName = "findByAvailableFalse";
		Class<?>[] parameterClasses = new Class[] {};
		Object[] parameters = new Object[] {};

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"false\", \"fields\" : [\"available\"]}}" + //
				"    ]" + //
				"  }" + //
				"}}"; //

		assertEquals(expected, query, false);
	}

	@Test
	void findByAvailableTrueOrderByNameDesc() throws NoSuchMethodException, JSONException {
		String methodName = "findByAvailableTrueOrderByNameDesc";
		Class<?>[] parameterClasses = new Class[] {};
		Object[] parameters = new Object[] {};

		String query = getQueryString(methodName, parameterClasses, parameters);

		String expected = "{\"query\": {" + //
				"  \"bool\" : {" + //
				"    \"must\" : [" + //
				"      {\"query_string\" : {\"query\" : \"true\", \"fields\" : [\"available\"]}}" + //
				"    ]" + //
				"  }" + //
				"}," + //
				"\"sort\":[{\"name\":{\"order\":\"desc\"}}]" + //
				'}'; //

		assertEquals(expected, query, false);
	}

	private String getQueryString(String methodName, Class<?>[] parameterClasses, Object[] parameters)
			throws NoSuchMethodException {

		Method method = SampleRepository.class.getMethod(methodName, parameterClasses);
		ElasticsearchQueryMethod queryMethod = new ElasticsearchQueryMethod(method,
				new DefaultRepositoryMetadata(SampleRepository.class), new SpelAwareProxyProjectionFactory(),
				operations.getElasticsearchConverter().getMappingContext());
		ElasticsearchPartQuery partQuery = new ElasticsearchPartQuery(queryMethod, operations,
				QueryMethodEvaluationContextProvider.DEFAULT);
		Query query = partQuery.createQuery(parameters);
		return buildQueryString(query, Book.class);
	}

	/**
	 * builds the query String that would be sent to Elasticsearch
	 *
	 * @param query the {@link Query}
	 * @param clazz the entity class
	 * @return the created query string
	 */
	abstract protected String buildQueryString(Query query, Class<?> clazz);

	@FunctionalInterface
	interface AssertFunction {
		void get() throws JSONException;
	}

	private void assertAny(AssertFunction... funcs) {

		int failed = 0;

		for (AssertFunction func : funcs) {
			try {
				func.get();
			} catch (AssertionError | JSONException ignored) {
				failed++;
			}
		}

		if (failed == funcs.length) {
			throw new AssertionError("none of the assertions held");
		}
	}

	private interface SampleRepository extends ElasticsearchRepository<Book, String> {
		List<Book> findByName(String name);

		List<Book> findByNameNot(String name);

		List<Book> findByNameLike(String name);

		List<Book> findByNameStartingWith(String name);

		List<Book> findByNameEndingWith(String name);

		List<Book> findByNameContaining(String name);

		List<Book> findByNameIn(Collection<String> names);

		List<Book> findByNameNotIn(Collection<String> names);

		List<Book> findByNameAndPrice(String name, Integer price);

		List<Book> findByNameOrPrice(String name, Integer price);

		List<Book> findByPrice(Integer price);

		List<Book> findByPriceBetween(Integer lower, Integer upper);

		List<Book> findByPriceLessThan(Integer price);

		List<Book> findByPriceLessThanEqual(Integer price);

		List<Book> findByPriceGreaterThan(Integer price);

		List<Book> findByPriceGreaterThanEqual(Integer price);

		List<Book> findByPriceBefore(Integer price);

		List<Book> findByPriceAfter(Integer price);

		List<Book> findByAvailableTrue();

		List<Book> findByAvailableFalse();

		List<Book> findByAvailableTrueOrderByNameDesc();

	}

	public static class Book {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable private Integer price;
		@Field(type = FieldType.Boolean) private boolean available;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Nullable
		public Integer getPrice() {
			return price;
		}

		public void setPrice(Integer price) {
			this.price = price;
		}

		public Boolean getAvailable() {
			return available;
		}

		public void setAvailable(Boolean available) {
			this.available = available;
		}
	}
}
