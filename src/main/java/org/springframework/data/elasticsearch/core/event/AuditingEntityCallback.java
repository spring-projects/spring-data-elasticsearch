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

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.Ordered;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.util.Assert;

/**
 * {@link EntityCallback} to populate auditing related fields on an entity about to be saved.
 *
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 * @since 4.0
 */
public class AuditingEntityCallback implements BeforeConvertCallback<Object>, Ordered {

	private final ObjectFactory<IsNewAwareAuditingHandler> auditingHandlerFactory;

	/**
	 * Creates a new {@link AuditingEntityCallback} using the given {@link IsNewAwareAuditingHandler} provided by the
	 * given {@link ObjectFactory}.
	 *
	 * @param auditingHandlerFactory must not be {@literal null}.
	 */
	public AuditingEntityCallback(ObjectFactory<IsNewAwareAuditingHandler> auditingHandlerFactory) {

		Assert.notNull(auditingHandlerFactory, "IsNewAwareAuditingHandler must not be null!");

		this.auditingHandlerFactory = auditingHandlerFactory;
	}

	@Override
	public Object onBeforeConvert(Object entity, IndexCoordinates index) {
		return auditingHandlerFactory.getObject().markAudited(entity);
	}

	@Override
	public int getOrder() {
		return 100;
	}
}
