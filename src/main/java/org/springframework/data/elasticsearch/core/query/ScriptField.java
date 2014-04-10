package org.springframework.data.elasticsearch.core.query;

import java.util.Map;

/**
 * @author Ryan Murfitt
 */
public class ScriptField {
    private final String fieldName;
    private final String script;
    private final Map<String, Object> params;

    public ScriptField(String fieldName, String script, Map<String, Object> params) {
        this.fieldName = fieldName;
        this.script = script;
        this.params = params;
    }

    public String fieldName() {
        return fieldName;
    }

    public String script() {
        return script;
    }

    public Map<String, Object> params() {
        return params;
    }
}

