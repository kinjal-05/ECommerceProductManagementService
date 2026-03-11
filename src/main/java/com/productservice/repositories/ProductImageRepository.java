package com.productservice.repositories;

import com.productservice.models.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage,Long>
{
	List<ProductImage> findByProductId(Long ProductId);
	void deleteByProductId(Long ProductId);
}