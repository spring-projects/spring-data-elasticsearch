package org.springframework.data.elasticsearch.core.completion;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Based on the reference doc - http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-suggesters-completion.html
 *
 * @author Mewes Kochheim
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class Completion {

	private String[] input;
	private String output;
	private Integer weight;
	private Object payload;

	private Completion() {
		//required by mapper to instantiate object
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

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}
}
