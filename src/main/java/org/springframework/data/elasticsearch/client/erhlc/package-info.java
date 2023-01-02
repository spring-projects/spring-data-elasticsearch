/*
 * Copyright 2022-2023 the original author or authors.
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

/**
 * This package contains classes that use the old Elasticsearch 7 libraries to access Elasticsearch either directly by
 * using the RestHighLevelClient or indirectly by using code copied from Elasticsearch libraries (reactive
 * implementation). These classes are deprectaed in favour of using the implementations using the new Elasticsearch
 * Client.
 */
@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
package org.springframework.data.elasticsearch.client.erhlc;
