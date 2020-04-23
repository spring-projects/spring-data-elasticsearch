/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.elasticsearch.core.client.support;

public class AliasData {
	private String filter = null;
	private String routing = null;
	private String search_routing = null;
	private String index_routing = null;

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public String getRouting() {
		return routing;
	}

	public void setRouting(String routing) {
		this.routing = routing;
	}

	public String getSearch_routing() {
		return search_routing;
	}

	public void setSearch_routing(String search_routing) {
		this.search_routing = search_routing;
	}

	public String getIndex_routing() {
		return index_routing;
	}

	public void setIndex_routing(String index_routing) {
		this.index_routing = index_routing;
	}
}
