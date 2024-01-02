/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.document;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.springframework.lang.Nullable;

/**
 * {@link SearchDocument} implementation using a {@link Document} delegate.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class SearchDocumentAdapter implements SearchDocument {

	private final float score;
	private final Object[] sortValues;
	private final Map<String, List<Object>> fields = new HashMap<>();
	private final Document delegate;
	private final Map<String, List<String>> highlightFields = new HashMap<>();
	private final Map<String, SearchDocumentResponse> innerHits = new HashMap<>();
	@Nullable private final NestedMetaData nestedMetaData;
	@Nullable private final Explanation explanation;
	@Nullable private final List<String> matchedQueries;
	@Nullable private final String routing;

	public SearchDocumentAdapter(Document delegate, float score, Object[] sortValues, Map<String, List<Object>> fields,
			Map<String, List<String>> highlightFields, Map<String, SearchDocumentResponse> innerHits,
			@Nullable NestedMetaData nestedMetaData, @Nullable Explanation explanation, @Nullable List<String> matchedQueries,
			@Nullable String routing) {

		this.delegate = delegate;
		this.score = score;
		this.sortValues = sortValues;
		this.fields.putAll(fields);
		this.highlightFields.putAll(highlightFields);
		this.innerHits.putAll(innerHits);
		this.nestedMetaData = nestedMetaData;
		this.explanation = explanation;
		this.matchedQueries = matchedQueries;
		this.routing = routing;
	}

	@Override
	public SearchDocument append(String key, Object value) {
		delegate.append(key, value);

		return this;
	}

	@Override
	public float getScore() {
		return score;
	}

	@Override
	public Map<String, List<Object>> getFields() {
		return fields;
	}

	@Override
	public Object[] getSortValues() {
		return sortValues;
	}

	@Override
	public Map<String, List<String>> getHighlightFields() {
		return highlightFields;
	}

	@Override
	public String getIndex() {
		return delegate.getIndex();
	}

	@Override
	public boolean hasId() {
		return delegate.hasId();
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public void setId(String id) {
		delegate.setId(id);
	}

	@Override
	public boolean hasVersion() {
		return delegate.hasVersion();
	}

	@Override
	public long getVersion() {
		return delegate.getVersion();
	}

	@Override
	public void setVersion(long version) {
		delegate.setVersion(version);
	}

	@Override
	public boolean hasSeqNo() {
		return delegate.hasSeqNo();
	}

	@Override
	public long getSeqNo() {
		return delegate.getSeqNo();
	}

	@Override
	public void setSeqNo(long seqNo) {
		delegate.setSeqNo(seqNo);
	}

	@Override
	public boolean hasPrimaryTerm() {
		return delegate.hasPrimaryTerm();
	}

	@Override
	public long getPrimaryTerm() {
		return delegate.getPrimaryTerm();
	}

	@Override
	public void setPrimaryTerm(long primaryTerm) {
		delegate.setPrimaryTerm(primaryTerm);
	}

	@Override
	public Map<String, SearchDocumentResponse> getInnerHits() {
		return innerHits;
	}

	@Override
	@Nullable
	public NestedMetaData getNestedMetaData() {
		return nestedMetaData;
	}

	@Override
	@Nullable
	public <T> T get(Object key, Class<T> type) {
		return delegate.get(key, type);
	}

	@Override
	public String toJson() {
		return delegate.toJson();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public Object get(Object key) {

		if (delegate.containsKey(key)) {
			return delegate.get(key);
		}

		// fallback to fields
		return fields.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return delegate.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return delegate.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<String> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<Object> values() {
		return delegate.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	@Nullable
	public Explanation getExplanation() {
		return explanation;
	}

	@Override
	@Nullable
	public List<String> getMatchedQueries() {
		return matchedQueries;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SearchDocumentAdapter that)) {
			return false;
		}
		return Float.compare(that.score, score) == 0 && delegate.equals(that.delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public void forEach(BiConsumer<? super String, ? super Object> action) {
		delegate.forEach(action);
	}

	@Override
	public boolean remove(Object key, Object value) {
		return delegate.remove(key, value);
	}

	@Override
	public String getRouting() {
		return routing;
	}

	@Override
	public String toString() {

		String id = hasId() ? getId() : "?";
		String version = hasVersion() ? Long.toString(getVersion()) : "?";

		return getClass().getSimpleName() + '@' + id + '#' + version + ' ' + toJson();
	}
}
