package org.springframework.data.elasticsearch;


import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.elasticsearch.repositories.NonDocumentEntityRepository;

public class NonDocumentEntityTest {


    @Test(expected = BeanCreationException.class)
    public void shouldNotInitialiseRepositoryWithNonDocument(){
        //when
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext("/repository-non-document-entity.xml");
        ctx.getBean(NonDocumentEntityRepository.class);
    }
}
