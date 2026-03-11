package com.productservice.repositories;

import com.productservice.models.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category>
{
	Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Category> findByDisplayOrder(Integer displayOrder, Pageable pageable);

	Page<Category> findByNameContainingIgnoreCaseAndDisplayOrder(
			String name,
			Integer displayOrder,
			Pageable pageable
	);
}