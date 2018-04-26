/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.data.elasticsearch.core.aggregation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.entities.StudentEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.junit.Assert.assertEquals;

/**
 * @author yushan gao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:elasticsearch-template-test.xml")
public class AggAssistantTests {

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(StudentEntity.class);
		elasticsearchTemplate.createIndex(StudentEntity.class);
		elasticsearchTemplate.putMapping(StudentEntity.class);
		elasticsearchTemplate.refresh(StudentEntity.class);

		List<StudentEntity> studentEntities = Arrays.asList(
				StudentEntity.builder()
						.id("1")
						.name("Tom")
						.gender("male")
						.age(12)
						.schoolName("Super School")
						.grade(3)
						.classNo(2)
						.build(),
				StudentEntity.builder()
						.id("2")
						.name("Jim")
						.gender("male")
						.age(13)
						.schoolName("Super School")
						.grade(2)
						.classNo(1)
						.build(),
				StudentEntity.builder()
						.id("3")
						.name("Lucy")
						.gender("female")
						.age(12)
						.schoolName("Super School")
						.grade(2)
						.classNo(1)
						.build(),
				StudentEntity.builder()
						.id("4")
						.name("Aaron")
						.gender("male")
						.age(12)
						.schoolName("Middle School")
						.grade(3)
						.classNo(2)
						.build(),
				StudentEntity.builder()
						.id("5")
						.name("David")
						.gender("female")
						.age(12)
						.schoolName("Middle School")
						.grade(3)
						.classNo(2)
						.build(),
				StudentEntity.builder()
						.id("6")
						.name("Edward")
						.gender("male")
						.age(12)
						.schoolName("Middle School")
						.grade(3)
						.classNo(2)
						.build()
		);

		for (StudentEntity studentEntity : studentEntities) {
			IndexQuery indexQuery = new IndexQuery();
			indexQuery.setId(studentEntity.getId());
			indexQuery.setObject(studentEntity);
			elasticsearchTemplate.index(indexQuery);
		}
		elasticsearchTemplate.refresh(StudentEntity.class);
	}

	@Test
	public void shouldReturnAggregatedResultGroupBySchoolGradeClassGenderWithAllCount() {
		AggValue avgAge = AggValue.value("avgAge", "age", AggregationBuilders::avg);
		AggList<GenderStatItem> genderAggAssistant = AggList.listField("gender", "gender",
				(value, subItems) -> new GenderStatItem(value, (int) AggList.getDocCount(subItems), avgAge.readValue(subItems)),
				avgAge);

		AggList<ClassStatItem> classAggAssistant = AggList.listField("class", "classNo",
				(value, subItems) -> ClassStatItem.builder()
						.classNo(Integer.parseInt(value))
						.statGenderByClass(genderAggAssistant.readValue(subItems))
						.total((int) AggList.getDocCount(subItems))
						.build(),
				genderAggAssistant);

		AggList<GradeStatItem> gradeAggAssistant = AggList.listField("grade", "grade",
				(value, subItems) -> GradeStatItem.builder()
						.grade(Integer.parseInt(value))
						.statGenderByGrade(genderAggAssistant.readValue(subItems))
						.classStatItems(classAggAssistant.readValue(subItems))
						.total((int) AggList.getDocCount(subItems))
						.build(),
				genderAggAssistant, classAggAssistant);

		AggList<SchoolStatItem> schoolAggAssistant = AggList.listField("school", "schoolName",
				(value, subItems) -> SchoolStatItem.builder()
						.schoolName(value)
						.statGenderBySchool(genderAggAssistant.readValue(subItems))
						.gradeStatItems(gradeAggAssistant.readValue(subItems))
						.total((int) AggList.getDocCount(subItems))
						.build(),
				genderAggAssistant, gradeAggAssistant);

		// given
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withSearchType(SearchType.DEFAULT)
				.withIndices("test-index-student").withTypes("test-student-type")
				.addAggregation(schoolAggAssistant.toAggBuilder())
				.build();
		// when
		List<SchoolStatItem> schoolStatItems = elasticsearchTemplate.query(searchQuery,
				new AggListExtractor<>(schoolAggAssistant));
		// then
		assertEquals("[{schoolName=Middle School,statGenderBySchool=[{gender=male,count=2,avgAge=12.0}, {gender=female,count=1,avgAge=12.0}],total=3,gradeStatItems=[{grade=3,statGenderByGrade=[{gender=male,count=2,avgAge=12.0}, {gender=female,count=1,avgAge=12.0}],total=3,classStatItems=[{classNo=2,statGenderByClass=[{gender=male,count=2,avgAge=12.0}, {gender=female,count=1,avgAge=12.0}],total=3}]}]}, {schoolName=Super School,statGenderBySchool=[{gender=male,count=2,avgAge=12.5}, {gender=female,count=1,avgAge=12.0}],total=3,gradeStatItems=[{grade=2,statGenderByGrade=[{gender=female,count=1,avgAge=12.0}, {gender=male,count=1,avgAge=13.0}],total=2,classStatItems=[{classNo=1,statGenderByClass=[{gender=female,count=1,avgAge=12.0}, {gender=male,count=1,avgAge=13.0}],total=2}]}, {grade=3,statGenderByGrade=[{gender=male,count=1,avgAge=12.0}],total=1,classStatItems=[{classNo=2,statGenderByClass=[{gender=male,count=1,avgAge=12.0}],total=1}]}]}]",
				schoolStatItems.toString());
	}

	@AllArgsConstructor
	public static class GenderStatItem {
		private String gender;
		private int count;
		private double avgAge;

		@Override
		public String toString() {
			return "{" +
					"gender=" + gender +
					",count=" + count +
					",avgAge=" + avgAge +
					'}';
		}
	}

	@AllArgsConstructor
	@Builder
	private static class ClassStatItem {
		private int classNo;
		private List<GenderStatItem> statGenderByClass;
		private int total;

		@Override
		public String toString() {
			return "{" +
					"classNo=" + classNo +
					",statGenderByClass=" + statGenderByClass +
					",total=" + total +
					'}';
		}
	}

	@AllArgsConstructor
	@Builder
	private static class GradeStatItem {
		private int grade;
		private List<GenderStatItem> statGenderByGrade;
		private int total;
		private List<ClassStatItem> classStatItems;

		@Override
		public String toString() {
			return "{" +
					"grade=" + grade +
					",statGenderByGrade=" + statGenderByGrade +
					",total=" + total +
					",classStatItems=" + classStatItems +
					'}';
		}
	}

	@AllArgsConstructor
	@Builder
	private static class SchoolStatItem {
		private String schoolName;
		private List<GenderStatItem> statGenderBySchool;
		private int total;
		private List<GradeStatItem> gradeStatItems;

		@Override
		public String toString() {
			return "{" +
					"schoolName=" + schoolName +
					",statGenderBySchool=" + statGenderBySchool +
					",total=" + total +
					",gradeStatItems=" + gradeStatItems +
					'}';
		}
	}
}


