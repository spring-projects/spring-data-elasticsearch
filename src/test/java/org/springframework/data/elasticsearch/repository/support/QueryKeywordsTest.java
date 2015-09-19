package org.springframework.data.elasticsearch.repository.support;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.entities.Product;
import org.springframework.data.elasticsearch.repositories.query.ProductRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artur Konczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/repository-query-support.xml")
public class QueryKeywordsTest {

	@Autowired
	private ProductRepository repository;

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Before
	public void before() {
		elasticsearchTemplate.deleteIndex(Product.class);
		elasticsearchTemplate.createIndex(Product.class);
		elasticsearchTemplate.putMapping(Product.class);
		elasticsearchTemplate.refresh(Product.class, true);

		repository.save(Arrays.asList(
				Product.builder().id("1").name("Sugar").text("Cane sugar").price(1.0f).available(false).build()
				, Product.builder().id("2").name("Sugar").text("Cane sugar").price(1.2f).available(true).build()
				, Product.builder().id("3").name("Sugar").text("Beet sugar").price(1.1f).available(true).build()
				, Product.builder().id("4").name("Salt").text("Rock salt").price(1.9f).available(true).build()
				, Product.builder().id("5").name("Salt").text("Sea salt").price(2.1f).available(false).build()));

		elasticsearchTemplate.refresh(Product.class, true);
	}

	@Test
	public void shouldSupportAND() {
		//given

		//when

		//then
		assertThat(repository.findByNameAndText("Sugar", "Cane sugar").size(), is(2));
		assertThat(repository.findByNameAndPrice("Sugar", 1.1f).size(), is(1));
	}

	@Test
	public void shouldSupportOR() {
		//given

		//when

		//then
		assertThat(repository.findByNameOrPrice("Sugar", 1.9f).size(), is(4));
		assertThat(repository.findByNameOrText("Salt", "Beet sugar").size(), is(3));
	}

	@Test
	public void shouldSupportTrueAndFalse() {
		//given

		//when

		//then
		assertThat(repository.findByAvailableTrue().size(), is(3));
		assertThat(repository.findByAvailableFalse().size(), is(2));
	}

	@Test
	public void shouldSupportInAndNotInAndNot() {
		//given

		//when

		//then
		assertThat(repository.findByPriceIn(Arrays.asList(1.2f, 1.1f)).size(), is(2));
		assertThat(repository.findByPriceNotIn(Arrays.asList(1.2f, 1.1f)).size(), is(3));
		assertThat(repository.findByPriceNot(1.2f).size(), is(4));
	}

	/*
	DATAES-171
	 */
	@Test
	public void shouldWorkWithNotIn() {
		//given

		//when

		//then
		assertThat(repository.findByIdNotIn(Arrays.asList("2", "3")).size(), is(3));
	}

	@Test
	public void shouldSupportBetween() {
		//given

		//when

		//then
		assertThat(repository.findByPriceBetween(1.0f, 2.0f).size(), is(4));
	}

	@Test
	public void shouldSupportLessThanAndGreaterThan() {
		//given

		//when

		//then
		assertThat(repository.findByPriceLessThan(1.1f).size(), is(1));
		assertThat(repository.findByPriceLessThanEqual(1.1f).size(), is(2));

		assertThat(repository.findByPriceGreaterThan(1.9f).size(), is(1));
		assertThat(repository.findByPriceGreaterThanEqual(1.9f).size(), is(2));
	}
}
