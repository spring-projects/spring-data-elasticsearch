package org.springframework.data.elasticsearch.core.jest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import io.searchbox.core.SearchResult;

/**
 * Extend SearchResult with useful result methods.
 * @author Julien Roy
 */
public class ExtendedSearchResult extends SearchResult {

	public ExtendedSearchResult(SearchResult searchResult) {
		super(searchResult);
	}

	public ExtendedSearchResult(Gson gson) {
		super(gson);
	}

	public String getScrollId() {
		return getJsonObject().get("_scroll_id").getAsString();
	}

	@Override
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
			Map<String, List<String>> highlight = extractHighlight(hitObject.getAsJsonObject(HIGHLIGHT_KEY));
			List<String> sort = extractSort(hitObject.getAsJsonArray(SORT_KEY));

			JsonObject source = hitObject.getAsJsonObject(sourceKey);
			if (source == null) {
				source = new JsonObject();
			}

			if (id != null) source.addProperty(ES_METADATA_ID, id.getAsString());
			hit = new Hit<T, K>(
					sourceType,
					source,
					explanationType,
					explanation,
					highlight,
					sort,
					index,
					type,
					score
			);

		}

		return hit;
	}
}
