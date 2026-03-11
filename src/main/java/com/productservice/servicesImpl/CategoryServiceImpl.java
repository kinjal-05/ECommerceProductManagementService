package com.productservice.servicesImpl;

import com.productservice.commondtos.CategoryEvent;
import com.productservice.dtos.CategoryCreateRequest;
import com.productservice.exceptions.ResourceNotFoundException;
import com.productservice.models.Category;
import com.productservice.publishEvent.CategoryEventProducer;
import com.productservice.repositories.CategoryRepository;
import com.productservice.services.CategoryService;
import com.productservice.specifications.CategorySpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;
	private final CategoryEventProducer producer;

	@Override
	@CircuitBreaker(name = "categoryCB", fallbackMethod = "createFallback")
	public Category create(CategoryCreateRequest request) {
		try {
			Category category = Category.builder().name(request.getName()).displayOrder(request.getDisplayOrder())
					.build();
			CategoryEvent event = CategoryEvent.builder().categoryId(category.getId()).name(category.getName()).build();
			producer.sendCategoryEvent(event);
			return categoryRepository.save(category);
		} catch (DataIntegrityViolationException ex) {
			throw new RuntimeException("Failed to create category due to database constraint violation", ex);
		} catch (Exception ex) {
			throw new RuntimeException("Unexpected error while creating category", ex);
		}
	}

	public Category createFallback(CategoryCreateRequest request, Throwable t) {
		throw new RuntimeException("Create category service is unavailable. Please try later.", t);
	}

	@Override
	@CircuitBreaker(name = "categoryCB", fallbackMethod = "getAllFallback")
	public Page<Category> getAll(Pageable pageable) {
		try {
			return categoryRepository.findAll(pageable);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch categories", ex);
		}
	}

	public Page<Category> getAllFallback(Pageable pageable, Throwable t) {
		throw new RuntimeException("Fetch categories service unavailable.", t);
	}

	@Override
	@CircuitBreaker(name = "categoryCB", fallbackMethod = "getByIdFallback")
	public Category getById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
	}

	public Category getByIdFallback(Long id, Throwable t) {
		throw new RuntimeException("Get category service unavailable for id: " + id, t);
	}

	@Override
	@CircuitBreaker(name = "categoryCB", fallbackMethod = "updateFallback")
	public Category update(Long id, CategoryCreateRequest request) {
		Category existingCategory = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
		Category updatedCategory = existingCategory.toBuilder().name(request.getName())
				.displayOrder(request.getDisplayOrder()).build();
		return categoryRepository.save(updatedCategory);
	}

	public Category updateFallback(Long id, CategoryCreateRequest request, Throwable t) {
		throw new RuntimeException("Update category service unavailable for id: " + id, t);
	}

	@Override
	@CircuitBreaker(name = "categoryCB", fallbackMethod = "deleteFallback")
	public void delete(Long id) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
		categoryRepository.delete(category);
	}

	public void deleteFallback(Long id, Throwable t) {
		throw new RuntimeException("Delete category service unavailable for id: " + id, t);
	}

	@Override
	@CircuitBreaker(name = "categoryCB", fallbackMethod = "searchFallback")
	public Page<Category> search(String name, Integer displayOrder, Pageable pageable) {
		Specification<Category> spec = CategorySpecifications.build(name, displayOrder);
		return categoryRepository.findAll(spec, pageable);
	}

	public Page<Category> searchFallback(String name, Integer displayOrder, Pageable pageable, Throwable t) {
		throw new RuntimeException("Search category service unavailable.", t);
	}
}