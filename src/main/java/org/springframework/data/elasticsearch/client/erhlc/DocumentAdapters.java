/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.client.erhlc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.document.Explanation;
import org.springframework.data.elasticsearch.core.document.NestedMetaData;
import org.springframework.data.elasticsearch.core.document.SearchDocument;
import org.springframework.data.elasticsearch.core.document.SearchDocumentResponse;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Utility class to adapt {@link org.elasticsearch.action.get.GetResponse},
 * {@link org.elasticsearch.index.get.GetResult}, {@link org.elasticsearch.action.get.MultiGetResponse}
 * {@link org.elasticsearch.search.SearchHit}, {@link org.elasticsearch.common.document.DocumentField} to
 * {@link Document}.
 *
 * @author Mark Paluch
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @author Matt Gilene
 * @since 4.0
 * @deprecated since 5.0
 */
@Deprecated
public final class DocumentAdapters {

	private DocumentAdapters() {}

	/**
	 * Create a {@link Document} from {@link GetResponse}.
	 * <p>
	 * Returns a {@link Document} using the getResponse if available.
	 *
	 * @param getResponse the getResponse {@link GetResponse}.
	 * @return the adapted {@link Document}, null if getResponse.isExists() returns false.
	 */
	@Nullable
	public static Document from(GetResponse getResponse) {

		Assert.notNull(getResponse, "GetResponse must not be null");

		if (!getResponse.isExists()) {
			return null;
		}

		if (getResponse.isSourceEmpty()) {
			return fromDocumentFields(getResponse, getResponse.getIndex(), getResponse.getId(), getResponse.getVersion(),
					getResponse.getSeqNo(), getResponse.getPrimaryTerm());
		}

		Document document = Document.from(getResponse.getSourceAsMap());
		document.setIndex(getResponse.getIndex());
		document.setId(getResponse.getId());
		document.setVersion(getResponse.getVersion());
		document.setSeqNo(getResponse.getSeqNo());
		document.setPrimaryTerm(getResponse.getPrimaryTerm());

		return document;
	}

	/**
	 * Create a {@link Document} from {@link GetResult}.
	 * <p>
	 * Returns a {@link Document} using the source if available.
	 *
	 * @param source the source {@link GetResult}.
	 * @return the adapted {@link Document}, null if source.isExists() returns false.
	 */
	@Nullable
	public static Document from(GetResult source) {

		Assert.notNull(source, "GetResult must not be null");

		if (!source.isExists()) {
			return null;
		}

		if (source.isSourceEmpty()) {
			return fromDocumentFields(source, source.getIndex(), source.getId(), source.getVersion(), source.getSeqNo(),
					source.getPrimaryTerm());
		}

		Document document = Document.from(source.getSource());
		document.setIndex(source.getIndex());
		document.setId(source.getId());
		document.setVersion(source.getVersion());
		document.setSeqNo(source.getSeqNo());
		document.setPrimaryTerm(source.getPrimaryTerm());

		return document;
	}

	/**
	 * Creates a List of {@link MultiGetItem<Document>}s from {@link MultiGetResponse}.
	 *
	 * @param source the source {@link MultiGetResponse}, not {@literal null}.
	 * @return a list of Documents, contains null values for not found Documents.
	 */
	public static List<MultiGetItem<Document>> from(MultiGetResponse source) {

		Assert.notNull(source, "MultiGetResponse must not be null");

		return Arrays.stream(source.getResponses()) //
				.map(DocumentAdapters::from) //
				.collect(Collectors.toList());
	}

	/**
	 * Creates a {@link MultiGetItem<Document>} from a {@link MultiGetItemResponse}.
	 *
	 * @param itemResponse the response, must not be {@literal null}
	 * @return the MultiGetItem
	 */
	public static MultiGetItem<Document> from(MultiGetItemResponse itemResponse) {

		MultiGetItem.Failure failure = ResponseConverter.getFailure(itemResponse);
		return MultiGetItem.of(itemResponse.isFailed() ? null : DocumentAdapters.from(itemResponse.getResponse()), failure);
	}

	/**
	 * Create a {@link SearchDocument} from {@link SearchHit}.
	 * <p>
	 * Returns a {@link SearchDocument} using the source if available.
	 *
	 * @param source the source {@link SearchHit}.
	 * @return the adapted {@link SearchDocument}.
	 */
	public static SearchDocument from(SearchHit source) {

		Assert.notNull(source, "SearchHit must not be null");

		Map<String, List<String>> highlightFields = new HashMap<>(source.getHighlightFields().entrySet().stream() //
				.collect(Collectors.toMap(Map.Entry::getKey,
						entry -> Arrays.stream(entry.getValue().getFragments()).map(Text::string).collect(Collectors.toList()))));

		Map<String, SearchDocumentResponse> innerHits = new LinkedHashMap<>();
		Map<String, SearchHits> sourceInnerHits = source.getInnerHits();

		if (sourceInnerHits != null) {
			sourceInnerHits.forEach((name, searchHits) -> innerHits.put(name, SearchDocumentResponseBuilder.from(searchHits,
					null, null, null, searchDocument -> CompletableFuture.completedFuture(null))));
		}

		NestedMetaData nestedMetaData = from(source.getNestedIdentity());
		Explanation explanation = from(source.getExplanation());
		List<String> matchedQueries = from(source.getMatchedQueries());

		BytesReference sourceRef = source.getSourceRef();
		Map<String, DocumentField> sourceFields = source.getFields();

		if (sourceRef == null || sourceRef.length() == 0) {
			return new SearchDocumentAdapter(
					fromDocumentFields(source, source.getIndex(), source.getId(), source.getVersion(), source.getSeqNo(),
							source.getPrimaryTerm()),
					source.getScore(), source.getSortValues(), sourceFields, highlightFields, innerHits, nestedMetaData,
					explanation, matchedQueries);
		}

		Document document = Document.from(source.getSourceAsMap());
		document.setIndex(source.getIndex());
		document.setId(source.getId());

		if (source.getVersion() >= 0) {
			document.setVersion(source.getVersion());
		}
		document.setSeqNo(source.getSeqNo());
		document.setPrimaryTerm(source.getPrimaryTerm());

		return new SearchDocumentAdapter(document, source.getScore(), source.getSortValues(), sourceFields, highlightFields,
				innerHits, nestedMetaData, explanation, matchedQueries);
	}

	@Nullable
	private static Explanation from(@Nullable org.apache.lucene.search.Explanation explanation) {

		if (explanation == null) {
			return null;
		}

		List<Explanation> details = new ArrayList<>();
		for (org.apache.lucene.search.Explanation detail : explanation.getDetails()) {
			details.add(from(detail));
		}

		return new Explanation(explanation.isMatch(), explanation.getValue().doubleValue(), explanation.getDescription(),
				details);
	}

	@Nullable
	private static NestedMetaData from(@Nullable SearchHit.NestedIdentity nestedIdentity) {

		if (nestedIdentity == null) {
			return null;
		}
		NestedMetaData child = from(nestedIdentity.getChild());
		return NestedMetaData.of(nestedIdentity.getField().string(), nestedIdentity.getOffset(), child);
	}

	@Nullable
	private static List<String> from(@Nullable String[] matchedQueries) {
		return matchedQueries == null ? null : Arrays.asList(matchedQueries);
	}

	/**
	 * Create an unmodifiable {@link Document} from {@link Iterable} of {@link DocumentField}s.
	 *
	 * @param documentFields the {@link DocumentField}s backing the {@link Document}.
	 * @param index the index where the Document was found
	 * @param id the document id
	 * @param version the document version
	 * @param seqNo the seqNo if the document
	 * @param primaryTerm the primaryTerm of the document
	 * @return the adapted {@link Document}.
	 */
	public static Document fromDocumentFields(Iterable<DocumentField> documentFields, String index, String id,
			long version, long seqNo, long primaryTerm) {

		if (documentFields instanceof Collection) {
			return new DocumentFieldAdapter((Collection<DocumentField>) documentFields, index, id, version, seqNo,
					primaryTerm);
		}

		List<DocumentField> fields = new ArrayList<>();
		for (DocumentField documentField : documentFields) {
			fields.add(documentField);
		}

		return new DocumentFieldAdapter(fields, index, id, version, seqNo, primaryTerm);
	}

	/**
	 * Adapter for a collection of {@link DocumentField}s.
	 */
	static class DocumentFieldAdapter implements Document {

		private final Collection<DocumentField> documentFields;
		private final Map<String, DocumentField> documentFieldMap;
		private final String index;
		private final String id;
		private final long version;
		private final long seqNo;
		private final long primaryTerm;

		DocumentFieldAdapter(Collection<DocumentField> documentFields, String index, String id, long version, long seqNo,
				long primaryTerm) {
			this.documentFields = documentFields;
			this.documentFieldMap = new LinkedHashMap<>(documentFields.size());
			documentFields.forEach(documentField -> documentFieldMap.put(documentField.getName(), documentField));
			this.index = index;
			this.id = id;
			this.version = version;
			this.seqNo = seqNo;
			this.primaryTerm = primaryTerm;
		}

		@Override
		public String getIndex() {
			return index;
		}

		@Override
		public boolean hasId() {
			return StringUtils.hasLength(id);
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public boolean hasVersion() {
			return this.version >= 0;
		}

		@Override
		public long getVersion() {

			if (!hasVersion()) {
				throw new IllegalStateException("No version associated with this Document");
			}

			return this.version;
		}

		@Override
		public boolean hasSeqNo() {
			return true;
		}

		@Override
		public long getSeqNo() {

			if (!hasSeqNo()) {
				throw new IllegalStateException("No seq_no associated with this Document");
			}

			return this.seqNo;
		}

		@Override
		public boolean hasPrimaryTerm() {
			return true;
		}

		@Override
		public long getPrimaryTerm() {

			if (!hasPrimaryTerm()) {
				throw new IllegalStateException("No primary_term associated with this Document");
			}

			return this.primaryTerm;
		}

		@Override
		public int size() {
			return documentFields.size();
		}

		@Override
		public boolean isEmpty() {
			return documentFields.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return documentFieldMap.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {

			for (DocumentField documentField : documentFields) {

				Object fieldValue = getValue(documentField);
				if (fieldValue != null && fieldValue.equals(value) || value == fieldValue) {
					return true;
				}
			}

			return false;
		}

		@Override
		@Nullable
		public Object get(Object key) {

			DocumentField documentField = documentFieldMap.get(key);
			return documentField != null ? documentField.getValue() : null;
		}

		@Override
		public Object put(String key, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends String, ?> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> keySet() {
			return documentFieldMap.keySet();
		}

		@Override
		public Collection<Object> values() {
			return documentFieldMap.values().stream().map(DocumentFieldAdapter::getValue).collect(Collectors.toList());
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			return documentFieldMap.entrySet().stream()
					.collect(Collectors.toMap(Entry::getKey, entry -> DocumentFieldAdapter.getValue(entry.getValue())))
					.entrySet();
		}

		@Override
		public void forEach(BiConsumer<? super String, ? super Object> action) {

			Objects.requireNonNull(action);

			documentFields.forEach(field -> action.accept(field.getName(), getValue(field)));
		}

		@Override
		public String toJson() {

			JsonFactory nodeFactory = new JsonFactory();
			try {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				JsonGenerator generator = nodeFactory.createGenerator(stream, JsonEncoding.UTF8);
				generator.writeStartObject();
				for (DocumentField value : documentFields) {
					if (value.getValues().size() > 1) {
						generator.writeArrayFieldStart(value.getName());
						for (Object val : value.getValues()) {
							generator.writeObject(val);
						}
						generator.writeEndArray();
					} else {
						generator.writeObjectField(value.getName(), value.getValue());
					}
				}
				generator.writeEndObject();
				generator.flush();
				return new String(stream.toByteArray(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new MappingException("Cannot render JSON", e);
			}
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + '@' + this.id + '#' + this.version + ' ' + toJson();
		}

		@Nullable
		private static Object getValue(DocumentField documentField) {

			if (documentField.getValues().isEmpty()) {
				return null;
			}

			if (documentField.getValues().size() == 1) {
				return documentField.getValue();
			}

			return documentField.getValues();
		}
	}

	/**
	 * Adapter for a {@link SearchDocument}.
	 */
	static class SearchDocumentAdapter implements SearchDocument {

		private final float score;
		private final Object[] sortValues;
		private final Map<String, List<Object>> fields = new HashMap<>();
		private final Document delegate;
		private final Map<String, List<String>> highlightFields = new HashMap<>();
		private final Map<String, SearchDocumentResponse> innerHits = new HashMap<>();
		@Nullable private final NestedMetaData nestedMetaData;
		@Nullable private final Explanation explanation;
		@Nullable private final List<String> matchedQueries;

		SearchDocumentAdapter(Document delegate, float score, Object[] sortValues, Map<String, DocumentField> fields,
				Map<String, List<String>> highlightFields, Map<String, SearchDocumentResponse> innerHits,
				@Nullable NestedMetaData nestedMetaData, @Nullable Explanation explanation,
				@Nullable List<String> matchedQueries) {

			this.delegate = delegate;
			this.score = score;
			this.sortValues = sortValues;
			fields.forEach((name, documentField) -> this.fields.put(name, documentField.getValues()));
			this.highlightFields.putAll(highlightFields);
			this.innerHits.putAll(innerHits);
			this.nestedMetaData = nestedMetaData;
			this.explanation = explanation;
			this.matchedQueries = matchedQueries;
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
		public String toString() {

			String id = hasId() ? getId() : "?";
			String version = hasVersion() ? Long.toString(getVersion()) : "?";

			return getClass().getSimpleName() + '@' + id + '#' + version + ' ' + toJson();
		}
	}
}
