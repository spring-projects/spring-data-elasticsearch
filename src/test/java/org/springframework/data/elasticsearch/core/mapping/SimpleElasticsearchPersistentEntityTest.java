package org.springframework.data.elasticsearch.core.mapping;

import org.junit.Test;
import org.springframework.data.elasticsearch.annotations.Version;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

public class SimpleElasticsearchPersistentEntityTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionGivenVersionPropertyIsNotLong() throws NoSuchFieldException, IntrospectionException {
        //given
        TypeInformation typeInformation = ClassTypeInformation.from(EntityWithWrongVersionType.class);
        SimpleElasticsearchPersistentProperty persistentProperty =
                new SimpleElasticsearchPersistentProperty(EntityWithWrongVersionType.class.getDeclaredField("version"),
                        new PropertyDescriptor("version", EntityWithWrongVersionType.class),
                        new SimpleElasticsearchPersistentEntity<EntityWithWrongVersionType>(typeInformation),
                        new SimpleTypeHolder());

        //when
        new SimpleElasticsearchPersistentEntity(typeInformation).addPersistentProperty(persistentProperty);
    }


    @Test(expected = MappingException.class)
    public void shouldThrowExceptionGivenMultipleVersionPropertiesArePresent() throws NoSuchFieldException, IntrospectionException {
        //given
        TypeInformation typeInformation = ClassTypeInformation.from(EntityWithMultipleVersionField.class);
        SimpleElasticsearchPersistentProperty persistentProperty1 =
                new SimpleElasticsearchPersistentProperty(EntityWithMultipleVersionField.class.getDeclaredField("version1"),
                        new PropertyDescriptor("version1", EntityWithMultipleVersionField.class),
                        new SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField>(typeInformation),
                        new SimpleTypeHolder());

        SimpleElasticsearchPersistentProperty persistentProperty2 =
                new SimpleElasticsearchPersistentProperty(EntityWithMultipleVersionField.class.getDeclaredField("version2"),
                        new PropertyDescriptor("version2", EntityWithMultipleVersionField.class),
                        new SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField>(typeInformation),
                        new SimpleTypeHolder());

        SimpleElasticsearchPersistentEntity simpleElasticsearchPersistentEntity = new SimpleElasticsearchPersistentEntity(typeInformation);
        simpleElasticsearchPersistentEntity.addPersistentProperty(persistentProperty1);
        //when
        simpleElasticsearchPersistentEntity.addPersistentProperty(persistentProperty2);
    }


    private class EntityWithWrongVersionType {
        @Version
        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    private class EntityWithMultipleVersionField{

        @Version
        private Long version1;
        @Version
        private Long version2;

        public Long getVersion1() {
            return version1;
        }

        public void setVersion1(Long version1) {
            this.version1 = version1;
        }

        public Long getVersion2() {
            return version2;
        }

        public void setVersion2(Long version2) {
            this.version2 = version2;
        }
    }

}
