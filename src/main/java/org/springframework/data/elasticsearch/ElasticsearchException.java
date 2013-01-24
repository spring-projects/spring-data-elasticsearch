package org.springframework.data.elasticsearch;


import java.util.Map;

public class ElasticsearchException extends RuntimeException{

    private Map<String, String> failedDocuments;

    public ElasticsearchException(String message) {
        super(message);
    }

    public ElasticsearchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ElasticsearchException(String message, Throwable cause, Map<String, String> failedDocuments) {
        super(message, cause);
        this.failedDocuments = failedDocuments;
    }

    public ElasticsearchException(String message, Map<String, String> failedDocuments) {
        super(message);
        this.failedDocuments = failedDocuments;
    }

    public Map<String, String> getFailedDocuments() {
        return failedDocuments;
    }
}
