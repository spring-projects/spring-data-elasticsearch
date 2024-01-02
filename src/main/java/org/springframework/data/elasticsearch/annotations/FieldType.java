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
package org.springframework.data.elasticsearch.annotations;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Artur Konczak
 * @author Zeng Zetang
 * @author Peter-Josef Meisch
 * @author Aleksei Arsenev
 * @author Brian Kimmig
 * @author Morgan Lutz
 */
public enum FieldType {
	Auto("auto"), //
	Text("text"), //
	Keyword("keyword"), //
	Long("long"), //
	Integer("integer"), //
	Short("short"), //
	Byte("byte"), //
	Double("double"), //
	Float("float"), //
	Half_Float("half_float"), //
	Scaled_Float("scaled_float"), //
	Date("date"), //
	Date_Nanos("date_nanos"), //
	Boolean("boolean"), //
	Binary("binary"), //
	Integer_Range("integer_range"), //
	Float_Range("float_range"), //
	Long_Range("long_range"), //
	Double_Range("double_range"), //
	Date_Range("date_range"), //
	Ip_Range("ip_range"), //
	Object("object"), //
	Nested("nested"), //
	Ip("ip"), //
	TokenCount("token_count"), //
	Percolator("percolator"), //
	Flattened("flattened"), //
	Search_As_You_Type("search_as_you_type"), //
	/** @since 4.1 */
	Rank_Feature("rank_feature"), //
	/** @since 4.1 */
	Rank_Features("rank_features"), //
	/** since 4.2 */
	Wildcard("wildcard"), //
	/** @since 4.2 */
	Dense_Vector("dense_vector"), //
	/**
	 * @since 5.2
	 */
	Constant_Keyword("constant_keyword"), //
	/**
	 * @since 5.2
	 */
	Alias("alias"), //
	/**
	 * @since 5.2
	 */
	Version("version"), //
	/**
	 * @since 5.2
	 */
	Murmur3("murmur3"), //
	/**
	 * @since 5.2
	 */
	Match_Only_Text("match_only_text"), //
	/**
	 * @since 5.2
	 */
	Annotated_Text("annotated_text") //
	;

	private final String mappedName;

	FieldType(String mappedName) {
		this.mappedName = mappedName;
	}

	public String getMappedName() {
		return mappedName;
	}
}
