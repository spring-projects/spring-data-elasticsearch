package org.springframework.data.elasticsearch.repositories.query;

import java.util.List;

import org.springframework.data.elasticsearch.entities.Product;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by akonczak on 04/09/15.
 */
public interface ProductRepository extends PagingAndSortingRepository<Product, String> {

	public List<Product> findByNameAndText(String name, String text);

	public List<Product> findByNameAndPrice(String name, Float price);

	public List<Product> findByNameOrText(String name, String text);

	public List<Product> findByNameOrPrice(String name, Float price);


	public List<Product> findByAvailableTrue();

	public List<Product> findByAvailableFalse();

	public List<Product> findByPriceIn(List<Float> floats);

	public List<Product> findByPriceNotIn(List<Float> floats);

	public List<Product> findByPriceNot(float v);

	public List<Product> findByPriceBetween(float v, float v1);

	public List<Product> findByPriceLessThan(float v);

	public List<Product> findByPriceLessThanEqual(float v);

	public List<Product> findByPriceGreaterThan(float v);

	public List<Product> findByPriceGreaterThanEqual(float v);

	public List<Product> findByIdNotIn(List<String> strings);
}
