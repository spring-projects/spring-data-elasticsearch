package org.springframework.data.elasticsearch.core.jest;

import com.google.gson.Gson;

import io.searchbox.client.JestResult;

/**
 * Represent Scroll result.
 * @author Julien Roy
 * @author Robert Gruendler
 */
public class ScrollSearchResult extends JestResult {

	public ScrollSearchResult(JestResult source) {
		super(source);
	}

	public ScrollSearchResult(Gson gson) {
		super(gson);
	}

	public String getScrollId() {
		return getJsonObject().get("_scroll_id").getAsString();
	}
}
