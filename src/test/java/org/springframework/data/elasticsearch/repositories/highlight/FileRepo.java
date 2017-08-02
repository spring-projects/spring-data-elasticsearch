/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.repositories.highlight;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.entities.File;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author zzt
 */
public interface FileRepo extends ElasticsearchRepository<File, String> {

	@Highlight(fields = { @HighlightField(name = "content", fragmentSize = 200, numOfFragments = 1) })
	Page<File> findByContent(String content, Pageable pageable);

	@Highlight(fields = { @HighlightField })
	List<File> findByTitle(String title);

	@Query("{\"match\": {\n" + "        \"title\": \"?0\"}}")
	@Highlight(fields = { @HighlightField(name = "content") })
	List<File> findByTitleButHighlightContent(String info);

	@Query(" {\"multi_match\": {\n        \"query\":    \"?0\",\n"
			+ "        \"fields\":   [ \"title^2\", \"content\" ]\n    }}")
	@Highlight(fields = { @HighlightField(name = "title", fragmentSize = 10, numOfFragments = 1) })
	Page<File> findByTitleOrContentHighlightTitle(String info, Pageable pageable);

	@Query(" {\"multi_match\": {\n        \"query\":    \"?0\",\n"
			+ "        \"fields\":   [ \"title^2\", \"content\" ]\n    }}")
	@Highlight(fields = { @HighlightField(name = "content", fragmentSize = 200, numOfFragments = 1) })
	List<File> findByTitleOrContentInListHighlightContent(String info);

	@Query(" {\"multi_match\": {\n        \"query\":    \"?0\",\n"
			+ "        \"fields\":   [ \"title^2\", \"content\" ]\n    }}")
	@Highlight(fields = { @HighlightField(name = "title", fragmentSize = 10, numOfFragments = 1),
			@HighlightField(name = "content", fragmentSize = 100, numOfFragments = 2) })
	List<File> findByTitleOrContentInListHighlightBoth(String info);
}
