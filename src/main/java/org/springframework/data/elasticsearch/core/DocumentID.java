package org.springframework.data.elasticsearch.core;

import java.io.Serializable;

/**
 * @author Murali Chevuri
 */
public interface DocumentID extends Serializable {
	String getDocumentId();
}
