package org.springframework.data.elasticsearch;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Version;

@Document(indexName = "test-index", type = "test-type")
public class SampleEntity {

    @Id
    private String id;
    private String type;
    private String message;
    private int rate;
    private boolean available;
    @Version
    private Long version;

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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
                .append(this.rate,rhs.rate)
                .append(this.available,rhs.available)
                .append(this.version,rhs.version)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(type)
                .append(message)
                .append(rate)
                .append(available)
                .append(version)
                .toHashCode();
    }
}
