package org.springframework.data.elasticsearch.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * @author abdul.
 */
@Document(indexName = "test-index-2", type =  "hetro", replicas = 0, shards = 1)
public class HetroEntity2 {

    @Id
    private String id;
    private String lastName;
    @Version
    private Long version;

    public HetroEntity2(String id, String lastName) {
        this.id = id;
        this.lastName = lastName;
        this.version = System.currentTimeMillis();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getLastName() {
        return lastName;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SampleEntity)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        HetroEntity2 rhs = (HetroEntity2) obj;
        return new EqualsBuilder().append(this.id, rhs.id).append(this.lastName, rhs.lastName).append(this.version, rhs.version).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(lastName).append(version).toHashCode();
    }

}
