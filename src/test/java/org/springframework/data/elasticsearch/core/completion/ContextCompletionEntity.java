package org.springframework.data.elasticsearch.core.completion;

import org.elasticsearch.search.suggest.completion.context.ContextMapping;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionContext;
import org.springframework.data.elasticsearch.annotations.CompletionContextType;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * @author Mewes Kochheim
 * @author Robert Gruendler
 */
@Document(indexName = "test-index-context-completion", type = "context-completion-type", shards = 1, replicas = 0, refreshInterval = "-1")
public class ContextCompletionEntity {

	public static final String LANGUAGE_CATEGORY = "language";
	@Id
	private String id;
	private String name;

	@CompletionField(maxInputLength = 100, contexts = {
			@CompletionContext(name = LANGUAGE_CATEGORY, type = ContextMapping.Type.CATEGORY)
	})
	private Completion suggest;

	private ContextCompletionEntity() {
	}

	public ContextCompletionEntity(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Completion getSuggest() {
		return suggest;
	}

	public void setSuggest(Completion suggest) {
		this.suggest = suggest;
	}
}
