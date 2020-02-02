/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.event;

import java.util.Map;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

/**
 * Callback that is called before an entity is updated in the index.
 *
 * @author Peter-Josef Meisch
 * @since 4.0
 */
@FunctionalInterface
public interface BeforeUpdateCallback extends ElasticsearchEntityCallback<BeforeUpdateCallback.UpdateInfo> {

	UpdateInfo onBeforeUpdate(UpdateInfo updateInfo, String id, IndexCoordinates index);

	class UpdateInfo {

		private Map<String, Object> updates;

		private UpdateInfo(Map<String, Object> updates) {
			this.updates = updates;
		}

		public Map<String, Object> getUpdates() {
			return updates;
		}

		public static UpdateInfo of(Map<String, Object> updates) {
			return new UpdateInfo(updates);
		}
	}
}
