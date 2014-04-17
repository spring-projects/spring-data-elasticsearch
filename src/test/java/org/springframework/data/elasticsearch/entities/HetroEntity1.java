package org.springframework.data.elasticsearch.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * @author abdul.
 */
@Document(indexName = "test-index-1", type =  "hetro", replicas = 0, shards = 1)
public class HetroEntity1 {

    @Id
    private String id;
    private String firstName;
    @Version
    private Long version;

    public HetroEntity1(String id, String firstName) {
        this.id = id;
        this.firstName = firstName;
        this.version = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
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
        HetroEntity1 rhs = (HetroEntity1) obj;
        return new EqualsBuilder().append(this.id, rhs.id).append(this.firstName, rhs.firstName).append(this.version, rhs.version).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(firstName).append(version).toHashCode();
    }

}
