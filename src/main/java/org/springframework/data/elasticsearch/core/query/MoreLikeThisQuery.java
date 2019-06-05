/*
 * Copyright 2013-2019 the original author or authors.
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

import static java.util.Collections.addAll;
import static org.springframework.data.elasticsearch.core.query.AbstractQuery.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;

/**
 * MoreLikeThisQuery
 *
 * @author Rizwan Idrees
 * @author Mohsin Husen
 */
public class MoreLikeThisQuery {

	private String id;
	private String indexName;
	private String type;
	private List<String> searchIndices = new ArrayList<>();
	private List<String> searchTypes = new ArrayList<>();
	private List<String> fields = new ArrayList<>();
	private String routing;
	private Float percentTermsToMatch;
	private Integer minTermFreq;
	private Integer maxQueryTerms;
	private List<String> stopWords = new ArrayList<>();
	private Integer minDocFreq;
	private Integer maxDocFreq;
	private Integer minWordLen;
	private Integer maxWordLen;
	private Float boostTerms;
	private Pageable pageable = DEFAULT_PAGE;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getSearchIndices() {
		return searchIndices;
	}

	public void addSearchIndices(String... searchIndices) {
		addAll(this.searchIndices, searchIndices);
	}

	public List<String> getSearchTypes() {
		return searchTypes;
	}

	public void addSearchTypes(String... searchTypes) {
		addAll(this.searchTypes, searchTypes);
	}

	public List<String> getFields() {
		return fields;
	}

	public void addFields(String... fields) {
		addAll(this.fields, fields);
	}

	public String getRouting() {
		return routing;
	}

	public void setRouting(String routing) {
		this.routing = routing;
	}

	public Float getPercentTermsToMatch() {
		return percentTermsToMatch;
	}

	public void setPercentTermsToMatch(Float percentTermsToMatch) {
		this.percentTermsToMatch = percentTermsToMatch;
	}

	public Integer getMinTermFreq() {
		return minTermFreq;
	}

	public void setMinTermFreq(Integer minTermFreq) {
		this.minTermFreq = minTermFreq;
	}

	public Integer getMaxQueryTerms() {
		return maxQueryTerms;
	}

	public void setMaxQueryTerms(Integer maxQueryTerms) {
		this.maxQueryTerms = maxQueryTerms;
	}

	public List<String> getStopWords() {
		return stopWords;
	}

	public void addStopWords(String... stopWords) {
		addAll(this.stopWords, stopWords);
	}

	public Integer getMinDocFreq() {
		return minDocFreq;
	}

	public void setMinDocFreq(Integer minDocFreq) {
		this.minDocFreq = minDocFreq;
	}

	public Integer getMaxDocFreq() {
		return maxDocFreq;
	}

	public void setMaxDocFreq(Integer maxDocFreq) {
		this.maxDocFreq = maxDocFreq;
	}

	public Integer getMinWordLen() {
		return minWordLen;
	}

	public void setMinWordLen(Integer minWordLen) {
		this.minWordLen = minWordLen;
	}

	public Integer getMaxWordLen() {
		return maxWordLen;
	}

	public void setMaxWordLen(Integer maxWordLen) {
		this.maxWordLen = maxWordLen;
	}

	public Float getBoostTerms() {
		return boostTerms;
	}

	public void setBoostTerms(Float boostTerms) {
		this.boostTerms = boostTerms;
	}

	public Pageable getPageable() {
		return pageable;
	}

	public void setPageable(Pageable pageable) {
		this.pageable = pageable;
	}
}
