package org.springframework.data.elasticsearch.core.completion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Based on the reference doc -
 * https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters-completion.html
 *
 * @author Mewes Kochheim
 * @author Robert Gruendler
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Completion {

	private String[] input;
	private Map<String, List<String>> contexts;
	private Integer weight;

	private Completion() {
		// required by mapper to instantiate object
	}

	public Completion(String[] input) {
		this.input = input;
	}

	public String[] getInput() {
		return input;
	}

	public void setInput(String[] input) {
		this.input = input;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public Map<String, List<String>> getContexts() {
		return contexts;
	}

	public void setContexts(Map<String, List<String>> contexts) {
		this.contexts = contexts;
	}

}
