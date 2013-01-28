package org.springframework.data.elasticsearch;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "test-index", type = "test-type")
public class SampleEntity {

    @Id
    private String id;
    private String type;
    private String message;
    private int rate;
    private boolean available;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SampleEntity)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SampleEntity rhs = (SampleEntity) obj;
        return new EqualsBuilder().append(this.id, rhs.id)
                .append(this.type, rhs.type)
                .append(this.message, rhs.message)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(type)
                .append(message)
                .toHashCode();
    }
}
