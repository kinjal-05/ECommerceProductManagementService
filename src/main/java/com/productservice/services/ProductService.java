package com.productservice.services;

import com.productservice.dtos.ProductCreateRequest;
import com.productservice.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService
{
	Product createProduct(ProductCreateRequest product);
	Page<Product> getAllProducts(Pageable pageable);
	Product updateProduct(Long id,ProductCreateRequest product);
	void deleteProduct(Long id);
	Page<Product> searchProducts(
			String title,
			String author,
			String isbn,
			Long categoryId,
			Double minPrice,
			Double maxPrice,
			Pageable pageable
	);

	Product getProductById(Long id);
}