package com.productservice.services;

import com.productservice.dtos.CategoryCreateRequest;
import com.productservice.models.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService
{

	Category create(CategoryCreateRequest category);

	Page<Category> getAll(Pageable pageable);

	Category getById(Long id);

	Category update(Long id,CategoryCreateRequest category);

	void delete(Long id);

	Page<Category> search(String name, Integer displayOrder, Pageable pageable);
}