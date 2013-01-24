package org.springframework.data.elasticsearch;


import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "Foo")
public class NonDocumentEntity {

    private String someField1;
    private String someField2;

    public String getSomeField1() {
        return someField1;
    }

    public void setSomeField1(String someField1) {
        this.someField1 = someField1;
    }

    public String getSomeField2() {
        return someField2;
    }

    public void setSomeField2(String someField2) {
        this.someField2 = someField2;
    }
}
