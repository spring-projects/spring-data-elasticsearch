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
