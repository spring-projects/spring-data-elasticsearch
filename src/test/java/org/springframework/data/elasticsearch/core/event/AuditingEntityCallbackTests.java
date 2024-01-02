/*
 * Copyright 2020-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.lang.Nullable;

/**
 * @author Peter-Josef Meisch
 * @author Roman Puchkovskiy
 */
@ExtendWith(MockitoExtension.class)
class AuditingEntityCallbackTests {

	IsNewAwareAuditingHandler handler;
	AuditingEntityCallback callback;

	@BeforeEach
	void setUp() {
		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
		context.getPersistentEntity(Sample.class);
		handler = spy(new IsNewAwareAuditingHandler(PersistentEntities.of(context)));
		callback = new AuditingEntityCallback(() -> handler);
	}

	@Test // DATAES-68
	void shouldThrowExceptionOnNullFactory() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AuditingEntityCallback(null));
	}

	@Test // DATAES-68
	void shouldHaveOrder100() {
		assertThat(callback).isInstanceOf(Ordered.class);
		assertThat(callback.getOrder()).isEqualTo(100);
	}

	@Test // DATAES-68
	void shouldCallHandler() {
		Sample entity = new Sample();
		entity.setId("42");
		callback.onBeforeConvert(entity, IndexCoordinates.of("index"));

		verify(handler).markAudited(eq(entity));
	}

	@Test // DATAES-68
	void shouldReturnObjectFromHandler() {
		Sample sample1 = new Sample();
		sample1.setId("1");
		Sample sample2 = new Sample();
		sample2.setId("2");
		doReturn(sample2).when(handler).markAudited(any());

		Sample result = (Sample) callback.onBeforeConvert(sample1, IndexCoordinates.of("index"));

		assertThat(result).isSameAs(sample2);
	}

	static class Sample {

		@Nullable
		@Id String id;
		@Nullable
		@CreatedDate LocalDateTime createdDate;
		@Nullable
		@CreatedBy String createdBy;
		@Nullable
		@LastModifiedDate LocalDateTime modified;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Nullable
		public LocalDateTime getCreatedDate() {
			return createdDate;
		}

		public void setCreatedDate(LocalDateTime createdDate) {
			this.createdDate = createdDate;
		}

		@Nullable
		public String getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(@Nullable String createdBy) {
			this.createdBy = createdBy;
		}

		@Nullable
		public LocalDateTime getModified() {
			return modified;
		}

		public void setModified(LocalDateTime modified) {
			this.modified = modified;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Sample sample = (Sample) o;

			if (!Objects.equals(id, sample.id))
				return false;
			if (!Objects.equals(createdDate, sample.createdDate))
				return false;
			if (!Objects.equals(createdBy, sample.createdBy))
				return false;
			return Objects.equals(modified, sample.modified);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
			result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
			result = 31 * result + (modified != null ? modified.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "Sample{" + "id='" + id + '\'' + ", createdDate=" + createdDate + ", createdBy='" + createdBy + '\''
					+ ", modified=" + modified + '}';
		}
	}
}
