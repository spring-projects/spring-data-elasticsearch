package org.springframework.data.elasticsearch.core.jest;

import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestResult;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Search scroll result of elasticsearch action.
 * @author Julien Roy
 */
public class SearchScrollResult extends JestResult {

	public static final String EXPLANATION_KEY = "_explanation";
	public static final String HIGHLIGHT_KEY = "highlight";
	public static final String FIELDS_KEY = "fields";
	public static final String SORT_KEY = "sort";
	public static final String[] PATH_TO_TOTAL = "hits/total".split("/");
	public static final String[] PATH_TO_MAX_SCORE = "hits/max_score".split("/");

	public SearchScrollResult(JestResult source) {
		super(source);
	}

	public <T> List<Hit<T, Void>> getHits(Class<T> sourceType) {
		return getHits(sourceType, Void.class);
	}

	public <T, K> List<Hit<T, K>> getHits(Class<T> sourceType, Class<K> explanationType) {
		return getHits(sourceType, explanationType, false);
	}

	protected <T, K> List<Hit<T, K>> getHits(Class<T> sourceType, Class<K> explanationType, boolean returnSingle) {
		List<Hit<T, K>> sourceList = new ArrayList<Hit<T, K>>();

		if (jsonObject != null) {
			String[] keys = getKeys();
			if (keys != null) { // keys would never be null in a standard search scenario (i.e.: unless search class is overwritten)
				String sourceKey = keys[keys.length - 1];
				JsonElement obj = jsonObject.get(keys[0]);
				for (int i = 1; i < keys.length - 1; i++) {
					obj = ((JsonObject) obj).get(keys[i]);
				}

				if (obj.isJsonObject()) {
					sourceList.add(extractHit(sourceType, explanationType, obj, sourceKey));
				} else if (obj.isJsonArray()) {
					for (JsonElement hitElement : obj.getAsJsonArray()) {
						sourceList.add(extractHit(sourceType, explanationType, hitElement, sourceKey));
						if (returnSingle) {
							break;
						}
					}
				}
			}
		}

		return sourceList;
	}

	protected <T, K> Hit<T, K> extractHit(Class<T> sourceType, Class<K> explanationType, JsonElement hitElement, String sourceKey) {
		Hit<T, K> hit = null;

		if (hitElement.isJsonObject()) {
			JsonObject hitObject = hitElement.getAsJsonObject();

			JsonElement id = hitObject.get("_id");
			String index = hitObject.get("_index").getAsString();
			String type = hitObject.get("_type").getAsString();

			Double score = null;
			if (hitObject.has("_score") && !hitObject.get("_score").isJsonNull()) {
				score = hitObject.get("_score").getAsDouble();
			}

			JsonElement explanation = hitObject.get(EXPLANATION_KEY);
			Map<String, List<String>> highlight = extractJsonObject(hitObject.getAsJsonObject(HIGHLIGHT_KEY));
			Map<String, List<String>> fields = extractJsonObject(hitObject.getAsJsonObject(FIELDS_KEY));
			List<String> sort = extractSort(hitObject.getAsJsonArray(SORT_KEY));

			JsonObject source = hitObject.getAsJsonObject(sourceKey);
			if (source == null) {
				source = new JsonObject();
			}

			if (id != null) {
				source.add(ES_METADATA_ID, id);
			}
			hit = new Hit<T, K>(
					sourceType,
					source,
					explanationType,
					explanation,
					highlight,
					fields,
					sort,
					index,
					type,
					score
			);
		}

		return hit;
	}

	protected List<String> extractSort(JsonArray sort) {
		if (sort == null) {
			return null;
		}

		List<String> retval = new ArrayList<String>(sort.size());
		for (JsonElement sortValue : sort) {
			retval.add(sortValue.isJsonNull() ? "" : sortValue.getAsString());
		}
		return retval;
	}

	protected Map<String, List<String>> extractJsonObject(JsonObject highlight) {
		Map<String, List<String>> retval = null;

		if (highlight != null) {
			Set<Map.Entry<String, JsonElement>> highlightSet = highlight.entrySet();
			retval = new HashMap<String, List<String>>(highlightSet.size());

			for (Map.Entry<String, JsonElement> entry : highlightSet) {
				List<String> fragments = new ArrayList<String>();
				for (JsonElement element : entry.getValue().getAsJsonArray()) {
					fragments.add(element.getAsString());
				}
				retval.put(entry.getKey(), fragments);
			}
		}

		return retval;
	}

	public Integer getTotal() {
		Integer total = null;
		JsonElement obj = getPath(PATH_TO_TOTAL);
		if (obj != null) {
			total = obj.getAsInt();
		}
		return total;
	}

	public Float getMaxScore() {
		Float maxScore = null;
		JsonElement obj = getPath(PATH_TO_MAX_SCORE);
		if (obj != null) maxScore = obj.getAsFloat();
		return maxScore;
	}

	protected JsonElement getPath(String[] path) {
		JsonElement retval = null;
		if (jsonObject != null) {
			JsonElement obj = jsonObject;
			for (String component : path) {
				if (obj == null) {
					break;
				}
				obj = ((JsonObject) obj).get(component);
			}
			retval = obj;
		}
		return retval;
	}

	public String getScrollId() {
		return jsonObject.get("_scroll_id").getAsString();
	}


	/**
	 * Immutable class representing a search hit.
	 *
	 * @param <T> type of source
	 * @param <K> type of explanation
	 * @author cihat keser
	 */
	public class Hit<T, K> {

		public final T source;
		public final K explanation;
		public final Map<String, List<String>> highlight;
		public final Map<String, List<String>> fields;
		public final List<String> sort;
		public final String index;
		public final String type;
		public final Double score;

		public Hit(Class<T> sourceType, JsonElement source) {
			this(sourceType, source, null, null);
		}

		public Hit(Class<T> sourceType, JsonElement source, Class<K> explanationType, JsonElement explanation) {
			this(sourceType, source, explanationType, explanation, null, null, null, null, null, null);
		}

		public Hit(Class<T> sourceType, JsonElement source, Class<K> explanationType, JsonElement explanation,
				   Map<String, List<String>> highlight, Map<String, List<String>> fields, List<String> sort,
				   String index, String type, Double score) {
			if (source == null) {
				this.source = null;
			} else {
				this.source = createSourceObject(source, sourceType);
			}
			if (explanation == null) {
				this.explanation = null;
			} else {
				this.explanation = createSourceObject(explanation, explanationType);
			}
			this.highlight = highlight;
			this.fields = fields;
			this.sort = sort;

			this.index = index;
			this.type = type;
			this.score = score;
		}

		public Hit(T source) {
			this(source, null, null, null, null, null, null, null);
		}

		public Hit(T source, K explanation) {
			this(source, explanation, null, null, null, null, null, null);
		}

		public Hit(T source, K explanation, Map<String, List<String>> highlight, Map<String, List<String>> fields, List<String> sort, String index, String type, Double score) {
			this.source = source;
			this.explanation = explanation;
			this.highlight = highlight;
			this.fields = fields;
			this.sort = sort;

			this.index = index;
			this.type = type;
			this.score = score;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder()
					.append(source)
					.append(explanation)
					.append(highlight)
					.append(fields)
					.append(sort)
					.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj.getClass() != getClass()) {
				return false;
			}

			Hit rhs = (Hit) obj;
			return new EqualsBuilder()
					.append(source, rhs.source)
					.append(explanation, rhs.explanation)
					.append(highlight, rhs.highlight)
					.append(fields, rhs.fields)
					.append(sort, rhs.sort)
					.isEquals();
		}
	}

}