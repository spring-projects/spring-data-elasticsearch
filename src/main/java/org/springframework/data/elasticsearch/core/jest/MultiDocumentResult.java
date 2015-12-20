package org.springframework.data.elasticsearch.core.jest;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestResult;

/**
 * Bulk result of elasticsearch action.
 *
 * @author Julien Roy
 */
public class MultiDocumentResult extends JestResult {

	public MultiDocumentResult(JestResult source) {
		super(source);
	}

	/**
	 * @return empty list if Bulk action failed on HTTP level, otherwise all individual action items in the response
	 */
	public List<MultiDocumentResultItem> getItems() {
		List<MultiDocumentResultItem> items = new LinkedList<MultiDocumentResultItem>();

		if (jsonObject != null && jsonObject.has("docs")) {
			for (JsonElement jsonElement : jsonObject.getAsJsonArray("docs")) {
				items.add(new MultiDocumentResultItem(jsonElement));
			}
		}

		return items;
	}

	/**
	 * MultiDocument Item.
	 */
	public static class MultiDocumentResultItem {

		private final JsonObject jsonObject;

		public MultiDocumentResultItem(JsonElement jsonElement) {
			this.jsonObject = jsonElement.getAsJsonObject();
		}

		public String getId() {
			return getAsString(jsonObject.get("_id"));
		}

		private String getAsString(JsonElement jsonElement) {
			if (jsonElement == null) {
				return null;
			} else {
				return jsonElement.getAsString();
			}
		}

		public String getSource() {
			return jsonObject.get("_source").toString();
		}
	}
}
