package org.springframework.data.elasticsearch.core.query;

import org.elasticsearch.script.Script;

/**
 * @author Ryan Murfitt
 * @author Artur Konczak
 */
public class ScriptField {

	private final String fieldName;
	private final Script script;

	public ScriptField(String fieldName, Script script) {
		this.fieldName = fieldName;
		this.script = script;
	}

	public String fieldName() {
		return fieldName;
	}

	public Script script() {
		return script;
	}
}
