/*
 * Copyright 2022-present the original author or authors.
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
package org.springframework.data.elasticsearch.core.query;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.function.Function;

/**
 * value class combining script information.
 * <p>
 * A script is either an inline script, then the script parameters must be set
 * or it refers to a stored script, then the name parameter is required.
 *
 * @param language   the language when the script is passed in the script parameter
 * @param script     the script to use as inline script
 * @param scriptName the name when using a stored script
 * @param params     the script parameters
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public record ScriptData(@Nullable String language, @Nullable String script,
                         @Nullable String scriptName, @Nullable Map<String, Object> params) {

    /*
     * constructor overload to check the parameters
     */
    public ScriptData(@Nullable String language, @Nullable String script, @Nullable String scriptName,
                      @Nullable Map<String, Object> params) {

        Assert.isTrue(script != null || scriptName != null, "script or scriptName is required");

        this.language = language;
        this.script = script;
        this.scriptName = scriptName;
        this.params = params;
    }

    /**
     * factory method to create a ScriptData object.
     *
     * @since 5.2
     */
    public static ScriptData of(@Nullable String language, @Nullable String script,
                                @Nullable String scriptName, @Nullable Map<String, Object> params) {
        return new ScriptData(language, script, scriptName, params);
    }

    /**
     * factory method to create a ScriptData  object using a ScriptBuilder callback.
     *
     * @param builderFunction function called to populate the builder
     * @return
     */
    public static ScriptData of(Function<Builder, Builder> builderFunction) {

        Assert.notNull(builderFunction, "builderFunction must not be null");

        return builderFunction.apply(new Builder()).build();
    }

    /**
     * @since 5.2
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @since 5.2
     */
    public static final class Builder {
        @Nullable
        private String language;
        @Nullable
        private String script;
        @Nullable
        private String scriptName;
        @Nullable
        private Map<String, Object> params;

        private Builder() {
        }

        public Builder withLanguage(@Nullable String language) {
            this.language = language;
            return this;
        }

        public Builder withScript(@Nullable String script) {
            this.script = script;
            return this;
        }

        public Builder withScriptName(@Nullable String scriptName) {
            this.scriptName = scriptName;
            return this;
        }

        public Builder withParams(@Nullable Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public ScriptData build() {
            return new ScriptData(language, script, scriptName, params);
        }
    }
}
