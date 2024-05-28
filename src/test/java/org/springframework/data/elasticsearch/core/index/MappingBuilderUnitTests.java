/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.data.elasticsearch.core.index;

import static org.assertj.core.api.Assertions.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;
import static org.springframework.data.elasticsearch.annotations.FieldType.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Range;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.MappingContextBaseTests;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;

/**
 * @author Stuart Stevenson
 * @author Jakub Vavrik
 * @author Mohsin Husen
 * @author Keivn Leturc
 * @author Nordine Bittich
 * @author Don Wellington
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 * @author Xiao Yu
 * @author Roman Puchkovskiy
 * @author Brian Kimmig
 * @author Morgan Lutz
 * @author Haibo Liu
 */
public class MappingBuilderUnitTests extends MappingContextBaseTests {

	@Test // DATAES-568
	public void testInfiniteLoopAvoidance() throws JSONException {

		String expected = """
				{
						"properties": {
							"message": {
								"store": true,
								"type": "text",
								"index": false,
								"analyzer": "standard"
							}
						}
					}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(SampleTransientEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseValueFromAnnotationType() throws JSONException {

		String expected = """
				  {
				  "properties": {
				    "price": {
				      "type": "double"
				    }
				  }
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(StockPrice.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-76
	public void shouldBuildMappingWithSuperclass() throws JSONException {

		String expected = """
				{
						"properties": {
							"message": {
								"store": true,
								"type": "text",
								"index": false,
								"analyzer": "standard"
							},
							"createdDate": {
								"type": "date",
								"index": false
							}
						}
					}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(SampleInheritedEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-285
	public void shouldMapBooks() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "author": {
				      "type": "object",
				      "properties": {}
				    },
				    "buckets": {
				      "type": "nested"
				    },
				    "description": {
				      "type": "text",
				      "analyzer": "whitespace",
				      "fields": {
				        "prefix": {
				          "type": "text",
				          "analyzer": "stop",
				          "search_analyzer": "standard"
				        }
				      }
				    }
				  }
				}"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(Book.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568, DATAES-929
	@DisplayName("should build mappings for geo types")
	void shouldBuildMappingsForGeoTypes() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "pointA": {
				      "type": "geo_point"
				    },
				    "pointB": {
				      "type": "geo_point"
				    },
				    "pointC": {
				      "type": "geo_point"
				    },
				    "pointD": {
				      "type": "geo_point"
				    },
				    "shape1": {
				      "type": "geo_shape"
				    },
				    "shape2": {
				      "type": "geo_shape",
				      "orientation": "clockwise",
				      "ignore_malformed": true,
				      "ignore_z_value": false,
				      "coerce": true
				    }
				  }
				}
				"""; //

		String mapping;
		mapping = getMappingBuilder().buildPropertyMapping(GeoEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnId() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "id-property": {
				      "type": "keyword",
				      "index": true
				    }
				  }
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.IdEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnText() throws JSONException {

		String expected = """
						{
					"properties": {
						"id-property": {
							"type": "keyword",
							"index": true
						},
						"text-property": {
							"type": "text"
						}
					}
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.TextEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnMapping() throws JSONException {

		String expected = """
					{
					"properties": {
						"id-property": {
							"type": "keyword",
							"index": true
						},
						"mapping-property": {
							"type": "string",
							"analyzer": "standard_lowercase_asciifolding"
						}
					}
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.MappingEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnGeoPoint() throws JSONException {

		String expected = """
				{
					"properties": {
						"id-property": {
							"type": "keyword",
							"index": true
						},
						"geopoint-property": {
							"type": "geo_point"
						}
					}
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.GeoPointEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnCircularEntity() throws JSONException {

		String expected = """
					{
					"properties": {
						"id-property": {
							"type": "keyword",
							"index": true
						},
						"circular-property": {
							"type": "object",
							"properties": {}
						}
					}
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.CircularEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568
	public void shouldUseFieldNameOnCompletion() throws JSONException {

		String expected = """
				{
					"properties": {
						"id-property": {
							"type": "keyword",
							"index": true
						},
						"completion-property": {
							"type": "completion",
							"max_input_length": 100,
							"preserve_position_increments": true,
							"preserve_separators": true,
							"search_analyzer": "simple",
							"analyzer": "simple"
						}
					}
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.CompletionEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-568, DATAES-896
	public void shouldUseFieldNameOnMultiField() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "id-property": {
				      "type": "keyword",
				      "index": true
				    },
				    "main-field": {
				      "type": "text",
				      "analyzer": "whitespace",
				      "fields": {
				        "suff-ix": {
				          "type": "text",
				          "analyzer": "stop",
				          "search_analyzer": "standard"
				        }
				      }
				    },
				    "alternate-description": {
				      "type": "text",
				      "analyzer": "whitespace",
				      "fields": {
				        "suff-ix": {
				          "type": "text",
				          "analyzer": "stop",
				          "search_analyzer": "standard"
				        }
				      }
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameEntity.MultiFieldEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-639
	public void shouldUseIgnoreAbove() throws JSONException {

		String expected = """
				{
					"properties": {
						"message": {
							"type": "keyword",
							"ignore_above": 10
						}
					}
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(IgnoreAboveEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-621, DATAES-943, DATAES-946
	public void shouldSetFieldMappingProperties() throws JSONException {
		String expected = """
				{
				        "properties": {
				            "storeTrue": {
				                "store": true
				            },
				            "indexFalse": {
				                "index": false
				            },
				            "coerceFalse": {
				                "coerce": false
				            },
				            "fielddataTrue": {
				                "fielddata": true
				            },
				            "type": {
				                "type": "integer"
				            },
				            "ignoreAbove": {
				                "ignore_above": 42
				            },
				            "copyTo": {
				                "copy_to": ["foo", "bar"]
				            },
				            "date": {
				                "type": "date",
				                "format": "YYYYMMDD"
				            },
				            "analyzers": {
				                "analyzer": "ana",
				                "search_analyzer": "sana",
				                "normalizer": "norma"
				            },
				            "docValuesTrue": {
				                "type": "keyword"
				            },
				            "docValuesFalse": {
				                "type": "keyword",
				                "doc_values": false
				            },
				            "ignoreMalformedTrue": {
				                "ignore_malformed": true
				            },
				            "indexPhrasesTrue": {
				                "index_phrases": true
				            },
				            "indexOptionsPositions": {
				                "index_options": "positions"
				            },
				            "defaultIndexPrefixes": {
				                "index_prefixes":{}            },
				            "customIndexPrefixes": {
				                "index_prefixes":{"min_chars":1,"max_chars":10}            },
				            "normsFalse": {
				                "norms": false
				            },
				            "nullValueString": {
				                "null_value": "NULLNULL"
				            },
				            "nullValueInteger": {
				                "null_value": 42
				            },
				            "nullValueDouble": {
				                "null_value": 42.0
				            },
				            "positionIncrementGap": {
				                "position_increment_gap": 42
				            },
				            "similarityBoolean": {
				                "similarity": "boolean"
				            },
				            "termVectorWithOffsets": {
				                "term_vector": "with_offsets"
				            },
				            "scaledFloat": {
				                "type": "scaled_float",
				                "scaling_factor": 100.0
				            },
				            "enabledObject": {
				                "type": "object"
				            },
				            "disabledObject": {
				                "type": "object",
				                "enabled": false
				            },
				            "eagerGlobalOrdinalsTrue": {
				                "type": "text",
				                "eager_global_ordinals": true
				            },
				            "eagerGlobalOrdinalsFalse": {
				                "type": "text"
				            },
				            "wildcardWithoutParams": {
				                "type": "wildcard"
				            },
				            "wildcardWithParams": {
				                "type": "wildcard",
				                "null_value": "WILD",
				                "ignore_above": 42
				            }
				        }
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(FieldMappingParameters.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-148, #1767
	void shouldWriteDynamicMappingFromAnnotation() throws JSONException {

		String expected = """
				{
				  "dynamic": "false",
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "author": {
				      "type": "object",
				      "dynamic": "strict",
				      "properties": {
				        "_class": {
				          "type": "keyword",
				          "index": false,
				          "doc_values": false
				        }
				      }
				    },
				    "objectMap": {
				      "type": "object",
				      "dynamic": "false"
				    },
				    "nestedObjectMap": {
				      "type": "nested",
				      "dynamic": "false"
				    }
				  }
				}"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DynamicMappingAnnotationEntity.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #1871
	void shouldWriteDynamicMapping() throws JSONException {

		String expected = """
				{
				  "dynamic": "false",
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "objectInherit": {
				      "type": "object"
				    },
				    "objectFalse": {
				      "dynamic": "false",
				      "type": "object"
				    },
				    "objectTrue": {
				      "dynamic": "true",
				      "type": "object"
				    },
				    "objectStrict": {
				      "dynamic": "strict",
				      "type": "object"
				    },
				    "objectRuntime": {
				      "dynamic": "runtime",
				      "type": "object"
				    },
				    "nestedObjectInherit": {
				      "type": "nested"
				    },
				    "nestedObjectFalse": {
				      "dynamic": "false",
				      "type": "nested"
				    },
				    "nestedObjectTrue": {
				      "dynamic": "true",
				      "type": "nested"
				    },
				    "nestedObjectStrict": {
				      "dynamic": "strict",
				      "type": "nested"
				    },
				    "nestedObjectRuntime": {
				      "dynamic": "runtime",
				      "type": "nested"
				    }
				  }
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(DynamicMappingEntity.class);

		assertEquals(expected, mapping, true);
	}

	@Test // DATAES-784
	void shouldMapPropertyObjectsToFieldDefinition() throws JSONException {
		String expected = """
				{
				  properties: {
				    valueObject: {
				      type: "text"
				    }
				  }
				}""";

		String mapping = getMappingBuilder().buildPropertyMapping(ValueDoc.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-788
	void shouldWriteCompletionContextInfo() throws JSONException {
		String expected = """
				{
				  "properties": {
				    "suggest": {
				      "type": "completion",
				      "contexts": [
				        {
				          "name": "location",
				          "type": "geo",
				          "path": "proppath"
				        }
				      ]
				    }
				  }
				}""";

		String mapping = getMappingBuilder().buildPropertyMapping(CompletionDocument.class);

		assertEquals(expected, mapping, false);
	}

	@Test // DATAES-799
	void shouldNotIncludeSeqNoPrimaryTermPropertyInMappingEvenWhenAnnotatedWithField() {
		String propertyMapping = getMappingBuilder().buildPropertyMapping(EntityWithSeqNoPrimaryTerm.class);

		assertThat(propertyMapping).doesNotContain("seqNoPrimaryTerm");
	}

	@Test // DATAES-854
	@DisplayName("should write rank_feature properties")
	void shouldWriteRankFeatureProperties() throws JSONException {
		String expected = """
				{
				  "properties": {
				    "pageRank": {
				      "type": "rank_feature"
				    },
				    "urlLength": {
				      "type": "rank_feature",
				      "positive_score_impact": false
				    },
				    "topics": {
				      "type": "rank_features"
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(RankFeatureEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // #1700
	@DisplayName("should write dense_vector properties")
	void shouldWriteDenseVectorProperties() throws JSONException {
		String expected = """
				{
				  "properties": {
				    "my_vector": {
				      "type": "dense_vector",
				      "dims": 16
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DenseVectorEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test
	@DisplayName("should write dense_vector properties for knn search")
	void shouldWriteDenseVectorPropertiesWithKnnSearch() throws JSONException {
		String expected = """
				{
				  "properties":{
				    "my_vector":{
				      "type":"dense_vector",
				      "dims":16,
				      "element_type":"float",
				      "similarity":"dot_product",
				      "index_options":{
				        "type":"hnsw",
				        "m":16,
				        "ef_construction":100
				      }
				    }
				  }
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(DenseVectorEntityWithKnnSearch.class);

		assertEquals(expected, mapping, false);
	}

	@Test // #1370
	@DisplayName("should not write mapping when enabled is false on entity")
	void shouldNotWriteMappingWhenEnabledIsFalseOnEntity() throws JSONException {

		String expected = """
				{
					"enabled": false
				}
				""";

		String mapping = getMappingBuilder().buildPropertyMapping(DisabledMappingEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // #1370
	@DisplayName("should write disabled property mapping")
	void shouldWriteDisabledPropertyMapping() throws JSONException {

		String expected = """
				{
				  "properties":{
				    "text": {
				      "type": "text"
				    },
				    "object": {
				      "type": "object",
				      "enabled": false
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DisabledMappingProperty.class);

		assertEquals(expected, mapping, false);
	}

	@Test // #1370
	@DisplayName("should only allow disabled properties on type object")
	void shouldOnlyAllowDisabledPropertiesOnTypeObject() {

		assertThatThrownBy(() -> getMappingBuilder().buildPropertyMapping(InvalidDisabledMappingProperty.class))
				.isInstanceOf(MappingException.class);
	}

	@Test
	@DisplayName("should match confidence interval parameter for dense_vector type")
	void shouldMatchConfidenceIntervalParameterForDenseVectorType() {

		assertThatThrownBy(() -> getMappingBuilder().buildPropertyMapping(DenseVectorMisMatchConfidenceIntervalClass.class))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test // #1711
	@DisplayName("should write typeHint entries")
	void shouldWriteTypeHintEntries() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "id": {
				      "type": "keyword"
				    },
				    "nestedEntity": {
				      "type": "nested",
				      "properties": {
				        "_class": {
				          "type": "keyword",
				          "index": false,
				          "doc_values": false
				        },
				        "nestedField": {
				          "type": "text"
				        }
				      }
				    },
				    "objectEntity": {
				      "type": "object",
				      "properties": {
				        "_class": {
				          "type": "keyword",
				          "index": false,
				          "doc_values": false
				        },
				        "objectField": {
				          "type": "text"
				        }
				      }
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(TypeHintEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // #1727
	@DisplayName("should map according to the annotated properties")
	void shouldMapAccordingToTheAnnotatedProperties() throws JSONException {

		String expected = """
				{
				    "properties": {
				        "field1": {
				            "type": "date",
				            "format": "date_optional_time||epoch_millis"
				        },
				        "field2": {
				            "type": "date",
				            "format": "basic_date"
				        },
				        "field3": {
				            "type": "date",
				            "format": "basic_date||basic_time"
				        },
				        "field4": {
				            "type": "date",
				            "format": "date_optional_time||epoch_millis||dd.MM.uuuu"
				        },
				        "field5": {
				            "type": "date",
				            "format": "dd.MM.uuuu"
				        }
				    }
				}"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DateFormatsEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // #2102
	@DisplayName("should write date formats for date range fields")
	void shouldWriteDateFormatsForDateRangeFields() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "field2": {
				      "type": "date_range",
				      "format": "date"
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DateRangeEntity.class);

		assertEquals(expected, mapping, false);
	}

	@Test // #1454
	@DisplayName("should write type hints when context is configured to do so")
	void shouldWriteTypeHintsWhenContextIsConfiguredToDoSo() throws JSONException {

		((SimpleElasticsearchMappingContext) (elasticsearchConverter.get().getMappingContext())).setWriteTypeHints(true);
		String expected = """
				{
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "title": {
				      "type": "text"
				    },
				    "authors": {
				      "type": "nested",
				      "properties": {
				        "_class": {
				          "type": "keyword",
				          "index": false,
				          "doc_values": false
				        }
				      }
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(Magazine.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #1454
	@DisplayName("should not write type hints when context is configured to not do so")
	void shouldNotWriteTypeHintsWhenContextIsConfiguredToNotDoSo() throws JSONException {

		((SimpleElasticsearchMappingContext) (elasticsearchConverter.get().getMappingContext())).setWriteTypeHints(false);
		String expected = """
				{
				  "properties": {
				    "title": {
				      "type": "text"
				    },
				    "authors": {
				      "type": "nested",
				      "properties": {
				      }
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(Magazine.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #1454
	@DisplayName("should write type hints when context is configured to not do so but entity should")
	void shouldWriteTypeHintsWhenContextIsConfiguredToNotDoSoButEntityShould() throws JSONException {

		((SimpleElasticsearchMappingContext) (elasticsearchConverter.get().getMappingContext())).setWriteTypeHints(false);
		String expected = """
				{
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "title": {
				      "type": "text"
				    },
				    "authors": {
				      "type": "nested",
				      "properties": {
				        "_class": {
				          "type": "keyword",
				          "index": false,
				          "doc_values": false
				        }
				      }
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(MagazineWithTypeHints.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #1454
	@DisplayName("should not write type hints when context is configured to do so but entity should not")
	void shouldNotWriteTypeHintsWhenContextIsConfiguredToDoSoButEntityShouldNot() throws JSONException {

		((SimpleElasticsearchMappingContext) (elasticsearchConverter.get().getMappingContext())).setWriteTypeHints(true);
		String expected = """
				{
				  "properties": {
				    "title": {
				      "type": "text"
				    },
				    "authors": {
				      "type": "nested",
				      "properties": {
				      }
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(MagazineWithoutTypeHints.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #638
	@DisplayName("should not write dynamic detection mapping entries in default setting")
	void shouldNotWriteDynamicDetectionMappingEntriesInDefaultSetting() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    }
				  }
				}"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DynamicDetectionMappingDefault.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #638
	@DisplayName("should write dynamic detection mapping entries when set to false")
	void shouldWriteDynamicDetectionMappingEntriesWhenSetToFalse() throws JSONException {

		String expected = """
				{
				  "date_detection": false,  "numeric_detection": false,  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    }
				  }
				}"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DynamicDetectionMappingFalse.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #638
	@DisplayName("should write dynamic detection mapping entries when set to true")
	void shouldWriteDynamicDetectionMappingEntriesWhenSetToTrue() throws JSONException {

		String expected = """
				{
				  "date_detection": true,  "numeric_detection": true,  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    }
				  }
				}"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DynamicDetectionMappingTrue.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #638
	@DisplayName("should write dynamic date formats")
	void shouldWriteDynamicDateFormats() throws JSONException {

		String expected = """
				{
				  "dynamic_date_formats": ["date1","date2"],  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    }
				  }
				}"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(DynamicDateFormatsMapping.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #1816
	@DisplayName("should write runtime fields")
	void shouldWriteRuntimeFields() throws JSONException {

		String expected = """
				{
				  "runtime": {
				    "day_of_week": {
				      "type": "keyword",
				      "script": {
				        "source": "emit(doc['@timestamp'].value.dayOfWeekEnum.getDisplayName(TextStyle.FULL, Locale.ROOT))"
				      }
				    }
				  },
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "@timestamp": {
				      "type": "date",
				      "format": "epoch_millis"
				    }
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(RuntimeFieldEntity.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #796
	@DisplayName("should add fields that are excluded from source")
	void shouldAddFieldsThatAreExcludedFromSource() throws JSONException {

		String expected = """
				{
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "excluded-date": {
				      "type": "date",
				      "format": "date"
				    },
				    "nestedEntity": {
				      "type": "nested",
				      "properties": {
				        "_class": {
				          "type": "keyword",
				          "index": false,
				          "doc_values": false
				        },
				        "excluded-text": {
				          "type": "text"
				        }
				      }
				    }
				  },
				  "_source": {
				    "excludes": [
				      "excluded-date",
				      "nestedEntity.excluded-text"
				    ]
				  }
				}
				"""; //

		String mapping = getMappingBuilder().buildPropertyMapping(ExcludedFieldEntity.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #2112
	@DisplayName("should not write mapping for property with IndexedIndexName anotation")
	void shouldNotWriteMappingForPropertyWithIndexedIndexNameAnotation() throws JSONException {

		var expected = """
				{
				  "properties": {
				    "_class": {
				      "type": "keyword",
				      "index": false,
				      "doc_values": false
				    },
				    "someText": {
				      "type": "text"
				    }
				  }
				}
				""";
		String mapping = getMappingBuilder().buildPropertyMapping(IndexedIndexNameEntity.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #2502
	@DisplayName("should use custom name with dots")
	void shouldUseCustomNameWithDots() throws JSONException {

		var expected = """
					{
					  "properties": {
					    "_class": {
					      "type": "keyword",
					      "index": false,
					      "doc_values": false
					    },
					    "dotted.field": {
					      "type": "text"
					    }
					  }
					}
				""";
		String mapping = getMappingBuilder().buildPropertyMapping(FieldNameDotsEntity.class);

		assertEquals(expected, mapping, true);
	}

	@Test // #2845
	@DisplayName("should write field aliases to the mapping")
	void shouldWriteFieldAliasesToTheMapping() throws JSONException {

		var expected = """
					{
					  "properties": {
					    "_class": {
					      "type": "keyword",
					      "index": false,
					      "doc_values": false
					    },
					    "someText": {
					      "type": "text"
					    },
					    "otherText": {
					      "type": "text"
					    },
					    "someAlly": {
					      "type": "alias",
					      "path": "someText"
					    },
					    "otherAlly": {
					      "type": "alias",
					      "path": "otherText"
					    }
					  }
					}
				""";
		String mapping = getMappingBuilder().buildPropertyMapping(FieldAliasEntity.class);

		assertEquals(expected, mapping, true);
	}
	// region entities

	@Document(indexName = "ignore-above-index")
	static class IgnoreAboveEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Keyword, ignoreAbove = 10) private String message;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}
	}

	static class FieldNameEntity {

		@Document(indexName = "fieldname-index")
		static class IdEntity {
			@Nullable
			@Id
			@Field("id-property") private String id;
		}

		@SuppressWarnings("unused")
		@Document(indexName = "fieldname-index")
		static class TextEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Field(name = "text-property", type = FieldType.Text) //
			@Nullable private String textProperty;
		}

		@SuppressWarnings("unused")
		@Document(indexName = "fieldname-index")
		static class MappingEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Field("mapping-property")
			@Mapping(mappingPath = "/mappings/test-field-analyzed-mappings.json") //
			@Nullable private byte[] mappingProperty;
		}

		@SuppressWarnings("unused")
		@Document(indexName = "fieldname-index")
		static class GeoPointEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable
			@Field("geopoint-property") private GeoPoint geoPoint;
		}

		@SuppressWarnings("unused")
		@Document(indexName = "fieldname-index")
		static class CircularEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable
			@Field(name = "circular-property", type = FieldType.Object, ignoreFields = { "circular-property" }) //
			private CircularEntity circularProperty;
		}

		@SuppressWarnings("unused")
		@Document(indexName = "fieldname-index")
		static class CompletionEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable
			@Field("completion-property")
			@CompletionField(maxInputLength = 100) //
			private Completion suggest;
		}

		@SuppressWarnings("unused")
		@Document(indexName = "fieldname-index")
		static class MultiFieldEntity {

			@Nullable
			@Id
			@Field("id-property") private String id;

			@Nullable
			@MultiField(mainField = @Field(name = "main-field", type = FieldType.Text, analyzer = "whitespace"),
					otherFields = {
							@InnerField(suffix = "suff-ix", type = FieldType.Text, analyzer = "stop", searchAnalyzer = "standard") }) //
			private String description;

			@Nullable private String alternateDescription;

			@Nullable
			@MultiField(mainField = @Field(name = "alternate-description", type = FieldType.Text, analyzer = "whitespace"),
					otherFields = {
							@InnerField(suffix = "suff-ix", type = FieldType.Text, analyzer = "stop", searchAnalyzer = "standard") }) //
			public String getAlternateDescription() {
				return alternateDescription;
			}

			public void setAlternateDescription(String alternateDescription) {
				this.alternateDescription = alternateDescription;
			}

		}
	}

	@Document(indexName = "test-index-book-mapping-builder")
	static class Book {
		@Nullable
		@Id private String id;
		@Nullable private String name;
		@Nullable
		@Field(type = FieldType.Object) private Author author;
		@Nullable
		@Field(type = FieldType.Nested) private Map<Integer, Collection<String>> buckets = new HashMap<>();
		@Nullable
		@MultiField(mainField = @Field(type = FieldType.Text, analyzer = "whitespace"),
				otherFields = { @InnerField(suffix = "prefix", type = FieldType.Text, analyzer = "stop",
						searchAnalyzer = "standard") }) private String description;

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
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(@Nullable Author author) {
			this.author = author;
		}

		@Nullable
		public Map<java.lang.Integer, Collection<String>> getBuckets() {
			return buckets;
		}

		public void setBuckets(@Nullable Map<java.lang.Integer, Collection<String>> buckets) {
			this.buckets = buckets;
		}

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-simple-recursive-mapping-builder")
	static class SimpleRecursiveEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Object, ignoreFields = { "circularObject" }) private SimpleRecursiveEntity circularObject;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public SimpleRecursiveEntity getCircularObject() {
			return circularObject;
		}

		public void setCircularObject(@Nullable SimpleRecursiveEntity circularObject) {
			this.circularObject = circularObject;
		}
	}

	@Document(indexName = "test-copy-to-mapping-builder")
	static class CopyToEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Keyword, copyTo = "name") private String firstName;
		@Nullable
		@Field(type = FieldType.Keyword, copyTo = "name") private String lastName;
		@Nullable
		@Field(type = FieldType.Keyword) private String name;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
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
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}

	@Document(indexName = "test-index-normalizer-mapping-builder")
	@Setting(settingPath = "/settings/test-normalizer.json")
	static class NormalizerEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Keyword, normalizer = "lower_case_normalizer") private String name;
		@Nullable
		@MultiField(mainField = @Field(type = FieldType.Text), otherFields = { @InnerField(suffix = "lower_case",
				type = FieldType.Keyword, normalizer = "lower_case_normalizer") }) private String description;

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
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}
	}

	static class Author {
		@Nullable private String id;
		@Nullable private String name;

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
	}

	@Document(indexName = "test-index-sample-inherited-mapping-builder")
	static class SampleInheritedEntity extends AbstractInheritedEntity {

		@Nullable
		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-stock-mapping-builder")
	static class StockPrice {
		@Nullable
		@Id private String id;
		@Nullable private String symbol;
		@Nullable
		@Field(type = FieldType.Double) private BigDecimal price;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getSymbol() {
			return symbol;
		}

		public void setSymbol(@Nullable String symbol) {
			this.symbol = symbol;
		}

		@Nullable
		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(@Nullable BigDecimal price) {
			this.price = price;
		}
	}

	@SuppressWarnings("unused")
	static class AbstractInheritedEntity {

		@Nullable
		@Id private String id;

		@Nullable
		@Field(type = FieldType.Date, format = DateFormat.date_time, index = false) private Date createdDate;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public Date getCreatedDate() {
			return createdDate;
		}

		public void setCreatedDate(Date createdDate) {
			this.createdDate = createdDate;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-recursive-mapping-mapping-builder")
	static class SampleTransientEntity {

		@Nullable
		@Id private String id;

		@Nullable
		@Field(type = Text, index = false, store = true, analyzer = "standard") private String message;

		@Nullable
		@Transient private NestedEntity nested;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		static class NestedEntity {

			@Field private static NestedEntity someField = new NestedEntity();
			@Nullable
			@Field private Boolean something;

			public NestedEntity getSomeField() {
				return someField;
			}

			public void setSomeField(NestedEntity someField) {
				NestedEntity.someField = someField;
			}

			@Nullable
			public Boolean getSomething() {
				return something;
			}

			public void setSomething(Boolean something) {
				this.something = something;
			}
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-geo-mapping-builder")
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
		@GeoShapeField private String shape1;
		@Nullable
		@GeoShapeField(coerce = true, ignoreMalformed = true, ignoreZValue = false,
				orientation = GeoShapeField.Orientation.clockwise) private String shape2;

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

		@Nullable
		public String getShape1() {
			return shape1;
		}

		public void setShape1(@Nullable String shape1) {
			this.shape1 = shape1;
		}

		@Nullable
		public String getShape2() {
			return shape2;
		}

		public void setShape2(@Nullable String shape2) {
			this.shape2 = shape2;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-field-mapping-parameters")
	static class FieldMappingParameters {
		@Nullable
		@Field private String indexTrue;
		@Nullable
		@Field(index = false) private String indexFalse;
		@Nullable
		@Field(store = true) private String storeTrue;
		@Nullable
		@Field private String storeFalse;
		@Nullable
		@Field private String coerceTrue;
		@Nullable
		@Field(coerce = false) private String coerceFalse;
		@Nullable
		@Field(fielddata = true) private String fielddataTrue;
		@Nullable
		@Field private String fielddataFalse;
		@Nullable
		@Field(copyTo = { "foo", "bar" }) private String copyTo;
		@Nullable
		@Field(ignoreAbove = 42) private String ignoreAbove;
		@Nullable
		@Field(type = FieldType.Integer) private String type;
		@Nullable
		@Field(type = FieldType.Date, format = {}, pattern = "YYYYMMDD") private LocalDate date;
		@Nullable
		@Field(analyzer = "ana", searchAnalyzer = "sana", normalizer = "norma") private String analyzers;
		@Nullable
		@Field(type = Keyword) private String docValuesTrue;
		@Nullable
		@Field(type = Keyword, docValues = false) private String docValuesFalse;
		@Nullable
		@Field(ignoreMalformed = true) private String ignoreMalformedTrue;
		@Nullable
		@Field() private String ignoreMalformedFalse;
		@Nullable
		@Field(indexOptions = IndexOptions.none) private String indexOptionsNone;
		@Nullable
		@Field(indexOptions = IndexOptions.positions) private String indexOptionsPositions;
		@Nullable
		@Field(indexPhrases = true) private String indexPhrasesTrue;
		@Nullable
		@Field() private String indexPhrasesFalse;
		@Nullable
		@Field(indexPrefixes = @IndexPrefixes) private String defaultIndexPrefixes;
		@Nullable
		@Field(indexPrefixes = @IndexPrefixes(minChars = 1, maxChars = 10)) private String customIndexPrefixes;
		@Nullable
		@Field private String normsTrue;
		@Nullable
		@Field(norms = false) private String normsFalse;
		@Nullable
		@Field private String nullValueNotSet;
		@Nullable
		@Field(nullValue = "NULLNULL") private String nullValueString;
		@Nullable
		@Field(nullValue = "42", nullValueType = NullValueType.Integer) private String nullValueInteger;
		@Nullable
		@Field(nullValue = "42.0", nullValueType = NullValueType.Double) private String nullValueDouble;
		@Nullable
		@Field(positionIncrementGap = 42) private String positionIncrementGap;
		@Nullable
		@Field private String similarityDefault;
		@Nullable
		@Field(similarity = Similarity.Boolean) private String similarityBoolean;
		@Nullable
		@Field private String termVectorDefault;
		@Nullable
		@Field(termVector = TermVector.with_offsets) private String termVectorWithOffsets;
		@Nullable
		@Field(type = FieldType.Scaled_Float, scalingFactor = 100.0) Double scaledFloat;
		@Nullable
		@Field(type = Auto) String autoField;
		@Nullable
		@Field(type = Object) private String enabledObject;
		@Nullable
		@Field(type = Object, enabled = false) private String disabledObject;
		@Nullable
		@Field(type = Text, eagerGlobalOrdinals = true) private String eagerGlobalOrdinalsTrue;
		@Nullable
		@Field(type = Text) private String eagerGlobalOrdinalsFalse;
		@Nullable
		@Field(type = Wildcard) private String wildcardWithoutParams;
		@Nullable
		@Field(type = Wildcard, nullValue = "WILD", ignoreAbove = 42) private String wildcardWithParams;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-configure-dynamic-mapping", dynamic = Dynamic.FALSE)
	static class DynamicMappingAnnotationEntity {

		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.STRICT) private Author author;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.FALSE) private Map<String, Object> objectMap;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.FALSE) private List<Map<String, Object>> nestedObjectMap;

		@Nullable
		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-configure-dynamic-mapping", dynamic = Dynamic.FALSE)
	static class DynamicMappingEntity {

		@Nullable
		@Field(type = FieldType.Object) //
		private Map<String, Object> objectInherit;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.FALSE) //
		private Map<String, Object> objectFalse;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.TRUE) //
		private Map<String, Object> objectTrue;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.STRICT) //
		private Map<String, Object> objectStrict;
		@Nullable
		@Field(type = FieldType.Object, dynamic = Dynamic.RUNTIME) //
		private Map<String, Object> objectRuntime;
		@Nullable
		@Field(type = FieldType.Nested) //
		private List<Map<String, Object>> nestedObjectInherit;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.FALSE) //
		private List<Map<String, Object>> nestedObjectFalse;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.TRUE) //
		private List<Map<String, Object>> nestedObjectTrue;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.STRICT) //
		private List<Map<String, Object>> nestedObjectStrict;
		@Nullable
		@Field(type = FieldType.Nested, dynamic = Dynamic.RUNTIME) //
		private List<Map<String, Object>> nestedObjectRuntime;

	}

	record ValueObject(String value) {
	}

	@SuppressWarnings("unused")
	@Document(indexName = "valueDoc")
	static class ValueDoc {
		@Nullable
		@Field(type = Text) private ValueObject valueObject;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "completion")
	static class CompletionDocument {
		@Nullable
		@Id private String id;
		@Nullable
		@CompletionField(contexts = { @CompletionContext(name = "location", type = CompletionContext.ContextMappingType.GEO,
				path = "proppath") }) private Completion suggest;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Completion getSuggest() {
			return suggest;
		}

		public void setSuggest(@Nullable Completion suggest) {
			this.suggest = suggest;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "test-index-entity-with-seq-no-primary-term-mapping-builder")
	static class EntityWithSeqNoPrimaryTerm {
		@Field(type = Object)
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;

		@Nullable
		public SeqNoPrimaryTerm getSeqNoPrimaryTerm() {
			return seqNoPrimaryTerm;
		}

		public void setSeqNoPrimaryTerm(SeqNoPrimaryTerm seqNoPrimaryTerm) {
			this.seqNoPrimaryTerm = seqNoPrimaryTerm;
		}
	}

	@SuppressWarnings("unused")
	static class RankFeatureEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Rank_Feature) private Integer pageRank;
		@Nullable
		@Field(type = FieldType.Rank_Feature, positiveScoreImpact = false) private Integer urlLength;
		@Nullable
		@Field(type = FieldType.Rank_Features) private Map<String, Integer> topics;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public java.lang.Integer getPageRank() {
			return pageRank;
		}

		public void setPageRank(@Nullable java.lang.Integer pageRank) {
			this.pageRank = pageRank;
		}

		@Nullable
		public java.lang.Integer getUrlLength() {
			return urlLength;
		}

		public void setUrlLength(@Nullable java.lang.Integer urlLength) {
			this.urlLength = urlLength;
		}

		@Nullable
		public Map<String, java.lang.Integer> getTopics() {
			return topics;
		}

		public void setTopics(@Nullable Map<String, java.lang.Integer> topics) {
			this.topics = topics;
		}
	}

	@SuppressWarnings("unused")
	static class DenseVectorEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Dense_Vector, dims = 16) private float[] my_vector;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public float[] getMy_vector() {
			return my_vector;
		}

		public void setMy_vector(@Nullable float[] my_vector) {
			this.my_vector = my_vector;
		}
	}

	@SuppressWarnings("unused")
	static class DenseVectorEntityWithKnnSearch {
		@Nullable
		@Id private String id;

		@Nullable
		@Field(type = FieldType.Dense_Vector, dims = 16, elementType = FieldElementType.FLOAT,
				knnIndexOptions = @KnnIndexOptions(type = KnnAlgorithmType.HNSW, m = 16, efConstruction = 100),
				knnSimilarity = KnnSimilarity.DOT_PRODUCT)
		private float[] my_vector;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public float[] getMy_vector() {
			return my_vector;
		}

		public void setMy_vector(@Nullable float[] my_vector) {
			this.my_vector = my_vector;
		}
	}

	@Mapping(enabled = false)
	static class DisabledMappingEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text) private String text;

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

	static class InvalidDisabledMappingProperty {
		@Nullable
		@Id private String id;
		@Nullable
		@Mapping(enabled = false)
		@Field(type = Text) private String text;

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

	static class DenseVectorMisMatchConfidenceIntervalClass {
		@Field(type = Dense_Vector, dims = 16, elementType = FieldElementType.FLOAT,
				knnIndexOptions = @KnnIndexOptions(type = KnnAlgorithmType.HNSW, m = 16, confidenceInterval = 0.95F),
				knnSimilarity = KnnSimilarity.DOT_PRODUCT)
		private float[] dense_vector;
	}

	static class DisabledMappingProperty {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Text) private String text;
		@Nullable
		@Mapping(enabled = false)
		@Field(type = Object) private Object object;

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

		@Nullable
		public java.lang.Object getObject() {
			return object;
		}

		public void setObject(@Nullable java.lang.Object object) {
			this.object = object;
		}
	}

	@SuppressWarnings("unused")
	static class TypeHintEntity {
		@Nullable
		@Id
		@Field(type = Keyword) private String id;
		@Nullable
		@Field(type = Nested) private NestedEntity nestedEntity;
		@Nullable
		@Field(type = Object) private ObjectEntity objectEntity;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public NestedEntity getNestedEntity() {
			return nestedEntity;
		}

		public void setNestedEntity(@Nullable NestedEntity nestedEntity) {
			this.nestedEntity = nestedEntity;
		}

		@Nullable
		public ObjectEntity getObjectEntity() {
			return objectEntity;
		}

		public void setObjectEntity(@Nullable ObjectEntity objectEntity) {
			this.objectEntity = objectEntity;
		}

		static class NestedEntity {
			@Nullable
			@Field(type = Text) private String nestedField;

			@Nullable
			public String getNestedField() {
				return nestedField;
			}

			public void setNestedField(@Nullable String nestedField) {
				this.nestedField = nestedField;
			}
		}

		static class ObjectEntity {
			@Nullable
			@Field(type = Text) private String objectField;

			@Nullable
			public String getObjectField() {
				return objectField;
			}

			public void setObjectField(@Nullable String objectField) {
				this.objectField = objectField;
			}
		}
	}

	@SuppressWarnings("unused")
	static class DateFormatsEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Date) private LocalDateTime field1;
		@Nullable
		@Field(type = FieldType.Date, format = DateFormat.basic_date) private LocalDateTime field2;
		@Nullable
		@Field(type = FieldType.Date,
				format = { DateFormat.basic_date, DateFormat.basic_time }) private LocalDateTime field3;
		@Nullable
		@Field(type = FieldType.Date, pattern = "dd.MM.uuuu") private LocalDateTime field4;
		@Nullable
		@Field(type = FieldType.Date, format = {}, pattern = "dd.MM.uuuu") private LocalDateTime field5;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public LocalDateTime getField1() {
			return field1;
		}

		public void setField1(@Nullable LocalDateTime field1) {
			this.field1 = field1;
		}

		@Nullable
		public LocalDateTime getField2() {
			return field2;
		}

		public void setField2(@Nullable LocalDateTime field2) {
			this.field2 = field2;
		}

		@Nullable
		public LocalDateTime getField3() {
			return field3;
		}

		public void setField3(@Nullable LocalDateTime field3) {
			this.field3 = field3;
		}

		@Nullable
		public LocalDateTime getField4() {
			return field4;
		}

		public void setField4(@Nullable LocalDateTime field4) {
			this.field4 = field4;
		}

		@Nullable
		public LocalDateTime getField5() {
			return field5;
		}

		public void setField5(@Nullable LocalDateTime field5) {
			this.field5 = field5;
		}
	}

	@SuppressWarnings("unused")
	private static class DateRangeEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = Date_Range, format = DateFormat.date) private Range<LocalDateTime> field2;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public Range<LocalDateTime> getField2() {
			return field2;
		}

		public void setField2(@Nullable Range<LocalDateTime> field2) {
			this.field2 = field2;
		}
	}

	@SuppressWarnings("unused")
	@Document(indexName = "magazine")
	private static class Magazine {
		@Id
		@Nullable private String id;
		@Field(type = Text)
		@Nullable private String title;
		@Field(type = Nested)
		@Nullable private List<Author> authors;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "magazine-without-type-hints", writeTypeHint = WriteTypeHint.FALSE)
	private static class MagazineWithoutTypeHints {
		@Id
		@Nullable private String id;
		@Field(type = Text)
		@Nullable private String title;
		@Field(type = Nested)
		@Nullable private List<Author> authors;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "magazine-with-type-hints", writeTypeHint = WriteTypeHint.TRUE)
	private static class MagazineWithTypeHints {
		@Id
		@Nullable private String id;
		@Field(type = Text)
		@Nullable private String title;
		@Field(type = Nested)
		@Nullable private List<Author> authors;
	}

	@Document(indexName = "dynamic-field-mapping-default")
	private static class DynamicDetectionMappingDefault {
		@Id
		@Nullable private String id;
	}

	@Document(indexName = "dynamic-dateformats-mapping")
	@Mapping(dynamicDateFormats = { "date1", "date2" })
	private static class DynamicDateFormatsMapping {
		@Id
		@Nullable private String id;
	}

	@Document(indexName = "dynamic-detection-mapping-true")
	@Mapping(dateDetection = Mapping.Detection.TRUE, numericDetection = Mapping.Detection.TRUE)
	private static class DynamicDetectionMappingTrue {
		@Id
		@Nullable private String id;
	}

	@Document(indexName = "dynamic-detection-mapping-false")
	@Mapping(dateDetection = Mapping.Detection.FALSE, numericDetection = Mapping.Detection.FALSE)
	private static class DynamicDetectionMappingFalse {
		@Id
		@Nullable private String id;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "runtime-fields")
	@Mapping(runtimeFieldsPath = "/mappings/runtime-fields.json")
	private static class RuntimeFieldEntity {
		@Id
		@Nullable private String id;
		@Field(type = Date, format = DateFormat.epoch_millis, name = "@timestamp")
		@Nullable private Instant timestamp;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "fields-excluded-from-source")
	private static class ExcludedFieldEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(name = "excluded-date", type = Date, format = DateFormat.date,
				excludeFromSource = true) private LocalDate excludedDate;
		@Nullable
		@Field(type = Nested) private NestedExcludedFieldEntity nestedEntity;
	}

	@SuppressWarnings("unused")
	private static class NestedExcludedFieldEntity {
		@Nullable
		@Field(name = "excluded-text", type = Text, excludeFromSource = true) private String excludedText;
	}

	@SuppressWarnings("unused")
	private static class IndexedIndexNameEntity {
		@Nullable
		@Field(type = Text) private String someText;
		@Nullable
		@IndexedIndexName private String storedIndexName;
	}

	@SuppressWarnings("unused")
	private static class FieldNameDotsEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(name = "dotted.field", type = Text) private String dottedField;
	}

	@Mapping(aliases = {
			@MappingAlias(name = "someAlly", path = "someText"),
			@MappingAlias(name = "otherAlly", path = "otherText")
	})
	private static class FieldAliasEntity {
		@Id
		@Nullable private String id;
		@Nullable
		@Field(type = Text) private String someText;
		@Nullable
		@Field(type = Text) private String otherText;
	}
	// endregion
}
