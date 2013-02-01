package org.springframework.data.elasticsearch;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.elasticsearch.repositories.NonDocumentEntityRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-non-document-entity.xml")
public class NonDocumentEntityTest {

    @Resource
    private NonDocumentEntityRepository nonDocumentEntityRepository;

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotIndexEntitiesWhichAreNotADocument(){
        //when
        nonDocumentEntityRepository.save(new NonDocumentEntity());
    }
}
