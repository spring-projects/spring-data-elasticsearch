/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core.mapping;

import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulating index mapping fields, settings, and index alias(es).
 *
 * @author Youssef Aouichaoui
 * @since 5.3
 */
public class CreateIndexSettings {
    private final IndexCoordinates indexCoordinates;
    private final Set<AliasCoordinates> aliasCoordinates;

    private final Map<String, Object> settings;

    @Nullable
    private final Document mapping;

    private CreateIndexSettings(Builder builder) {
        this.indexCoordinates = builder.indexCoordinates;
        this.aliasCoordinates = builder.aliasCoordinates;

        this.settings = builder.settings;
        this.mapping = builder.mapping;
    }

    public static Builder builder() {
        return new Builder();
    }

    public IndexCoordinates getIndexCoordinates() {
        return indexCoordinates;
    }

    public AliasCoordinates[] getAliasCoordinates() {
        return aliasCoordinates.toArray(AliasCoordinates[]::new);
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    @Nullable
    public Document getMapping() {
        return mapping;
    }

    public static class Builder {
        private IndexCoordinates indexCoordinates;
        private Set<AliasCoordinates> aliasCoordinates = new HashSet<>();

        private Map<String, Object> settings;

        @Nullable
        private Document mapping;

        public Builder withIndexCoordinates(IndexCoordinates indexCoordinates) {
            Assert.notNull(indexCoordinates, "indexCoordinates must not be null");
            this.indexCoordinates = indexCoordinates;

            return this;
        }

        public Builder withAliasCoordinates(AliasCoordinates aliasCoordinates) {
            Assert.notNull(aliasCoordinates, "aliasCoordinates must not be null");
            this.aliasCoordinates.add(aliasCoordinates);

            return this;
        }

        public Builder withAliases(Set<AliasCoordinates> aliases) {
            Assert.notNull(aliases, "aliases must not be null");
            this.aliasCoordinates.addAll(aliases);

            return this;
        }

        public Builder withSettings(Map<String, Object> settings) {
            Assert.notNull(settings, "settings must not be null");
            this.settings = settings;

            return this;
        }

        public Builder withMapping(@Nullable Document mapping) {
            this.mapping = mapping;

            return this;
        }

        public CreateIndexSettings build() {
            return new CreateIndexSettings(this);
        }
    }
}
