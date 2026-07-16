/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.data.elasticsearch.config;


/**
 * ElasticsearchServerType defines the type of Elasticsearch server you are connecting to.
 *  @see #DEFAULT
 *  @see #SERVERLESS
 *
 * @author Steven Pearce
 */
public enum ElasticsearchServerType {
  /**
   * Normal installations of Elasticsearch including cloud-hosted Elasticsearch
   */
  DEFAULT,
  /**
   * New Flavour of Elasticsearch, offered by Elastic
   */
  SERVERLESS,
}
