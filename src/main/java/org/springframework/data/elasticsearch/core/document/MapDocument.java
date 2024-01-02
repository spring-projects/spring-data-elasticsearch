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
package org.springframework.data.elasticsearch.core.document;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.springframework.data.elasticsearch.support.DefaultStringObjectMap;
import org.springframework.data.elasticsearch.support.StringObjectMap;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link Document} implementation backed by a {@link LinkedHashMap}.
 *
 * @author Mark Paluch
 * @author Roman Puchkovskiy
 * @since 4.0
 */
class MapDocument implements Document {

	static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final DefaultStringObjectMap<?> documentAsMap;

	private @Nullable String index;
	private @Nullable String id;
	private @Nullable Long version;
	private @Nullable Long seqNo;
	private @Nullable Long primaryTerm;

	MapDocument() {
		this(new LinkedHashMap<>());
	}

	MapDocument(Map<String, ?> documentAsMap) {
		this.documentAsMap = new DefaultStringObjectMap<>(documentAsMap);
	}

	@Override
	public void setIndex(@Nullable String index) {
		this.index = index;
	}

	@Nullable
	@Override
	public String getIndex() {
		return index;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#hasId()
	 */
	@Override
	public boolean hasId() {
		return this.id != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#getId()
	 */
	@Override
	public String getId() {

		if (!hasId()) {
			throw new IllegalStateException("No Id associated with this Document");
		}

		return this.id;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#setId(java.lang.String)
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#hasVersion()
	 */
	@Override
	public boolean hasVersion() {
		return this.version != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#getVersion()
	 */
	@Override
	public long getVersion() {

		if (!hasVersion()) {
			throw new IllegalStateException("No version associated with this Document");
		}

		return this.version;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#setVersion(long)
	 */
	@Override
	public void setVersion(long version) {
		this.version = version;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#hasSeqNo()
	 */
	@Override
	public boolean hasSeqNo() {
		return this.seqNo != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#getSeqNo()
	 */
	@Override
	public long getSeqNo() {

		if (!hasSeqNo()) {
			throw new IllegalStateException("No seq_no associated with this Document");
		}

		return this.seqNo;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#setSeqNo()
	 */
	public void setSeqNo(long seqNo) {
		this.seqNo = seqNo;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#hasPrimaryTerm()
	 */
	@Override
	public boolean hasPrimaryTerm() {
		return this.primaryTerm != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#getPrimaryTerm()
	 */
	@Override
	public long getPrimaryTerm() {

		if (!hasPrimaryTerm()) {
			throw new IllegalStateException("No primary_term associated with this Document");
		}

		return this.primaryTerm;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#setPrimaryTerm()
	 */
	public void setPrimaryTerm(long primaryTerm) {
		this.primaryTerm = primaryTerm;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		return documentAsMap.size();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return documentAsMap.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		return documentAsMap.containsKey(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		return documentAsMap.containsValue(value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public Object get(Object key) {
		return documentAsMap.get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#getOrDefault(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object getOrDefault(Object key, Object defaultValue) {
		return documentAsMap.getOrDefault(key, defaultValue);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object put(String key, Object value) {
		return documentAsMap.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public Object remove(Object key) {
		return documentAsMap.remove(key);
	}

	/*
		 * (non-Javadoc)
		 * @see java.util.Map#putAll(Map)
		 */
	@Override
	public void putAll(Map<? extends String, ?> m) {
		documentAsMap.putAll(m);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		documentAsMap.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<String> keySet() {
		return documentAsMap.keySet();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<Object> values() {
		return documentAsMap.values();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<Entry<String, Object>> entrySet() {
		return documentAsMap.entrySet();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		return documentAsMap.equals(o);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return documentAsMap.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#forEach(java.util.function.BiConsumer)
	 */
	@Override
	public void forEach(BiConsumer<? super String, ? super Object> action) {
		documentAsMap.forEach(action);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.elasticsearch.core.document.Document#toJson()
	 */
	@Override
	public String toJson() {
		try {
			return OBJECT_MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new MappingException("Cannot render document to JSON", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		String id = hasId() ? getId() : "?";
		String version = hasVersion() ? Long.toString(getVersion()) : "?";

		return getClass().getSimpleName() + '@' + id + '#' + version + ' ' + toJson();
	}
}
