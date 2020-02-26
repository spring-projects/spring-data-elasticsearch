package org.springframework.data.elasticsearch.core.completion;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.lang.Nullable;

/**
 * Based on the reference doc -
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters-completion.html
 *
 * @author Mewes Kochheim
 * @author Robert Gruendler
 * @author Peter-Josef Meisch
 */
public class Completion {

	private String[] input;
	@Nullable private Map<String, List<String>> contexts;
	@Nullable private Integer weight;

	public Completion(String[] input) {
		this.input = input;
	}

	@PersistenceConstructor
	public Completion(List<String> input) {
		this.input = input.toArray(new String[0]);
	}

	public String[] getInput() {
		return input;
	}

	public void setInput(String[] input) {
		this.input = input;
	}

	@Nullable
	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	@Nullable
	public Map<String, List<String>> getContexts() {
		return contexts;
	}

	public void setContexts(Map<String, List<String>> contexts) {
		this.contexts = contexts;
	}

}
