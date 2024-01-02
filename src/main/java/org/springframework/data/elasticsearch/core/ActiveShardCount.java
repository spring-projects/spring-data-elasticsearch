/*
 * Copyright 2021-2024 the original author or authors.
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
package org.springframework.data.elasticsearch.core;

/**
 * Class corresponding to the Elasticsearch class, but in the org.springframework.data.elasticsearch package
 *
 * @author Peter-Josef Meisch
 */
public record ActiveShardCount(int value) {
	private static final int ACTIVE_SHARD_COUNT_DEFAULT = -2;
	private static final int ALL_ACTIVE_SHARDS = -1;

	public static final ActiveShardCount DEFAULT = new ActiveShardCount(ACTIVE_SHARD_COUNT_DEFAULT);
	public static final ActiveShardCount ALL = new ActiveShardCount(ALL_ACTIVE_SHARDS);
	public static final ActiveShardCount NONE = new ActiveShardCount(0);
	public static final ActiveShardCount ONE = new ActiveShardCount(1);
}
