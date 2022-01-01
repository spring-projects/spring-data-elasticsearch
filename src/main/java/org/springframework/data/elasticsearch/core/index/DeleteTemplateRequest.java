/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.elasticsearch.core.index;

import org.springframework.util.Assert;

/**
 * @author Peter-Josef Meisch
 */
public class DeleteTemplateRequest {
	private final String templateName;

	public DeleteTemplateRequest(String templateName) {

		Assert.notNull(templateName, "templateName must not be null");

		this.templateName = templateName;
	}

	public String getTemplateName() {
		return templateName;
	}
}
