package com.productservice.controllers;

import com.productservice.dtos.CategoryCreateRequest;
import com.productservice.models.Category;
import com.productservice.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * All route paths resolved from api-paths.yml at startup.
 *
 * api.category.base       → /api/categories/v1
 * api.category.create     → /create
 * api.category.get-all    → /getAll
 * api.category.get-by-id  → /{id}
 * api.category.update-by-id → /{id}
 * api.category.delete-by-id → /{id}
 * api.category.search     → /search
 *
 * Bugs fixed from original:
 * - @RequestHeader + @PathVariable on same param → separated correctly
 * - @RequestHeader + @RequestParam on same param → removed header from pageable params
 * - Inconsistent header name (X-USER-ID used for email) → kept as-is to match gateway config
 * - System.out.println removed from production code
 */
@RestController
@RequestMapping("${api.category.base}")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@PostMapping("${api.category.create}")
	public ResponseEntity<Category> createCategory(
			@RequestHeader("X-USER-ID") String email,
			@Valid @RequestBody CategoryCreateRequest category) {

		Category savedCategory = categoryService.create(category);
		return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
	}

	@GetMapping("${api.category.get-all}")
	public ResponseEntity<Page<Category>> getAllCategories(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc")
				? Sort.by(sortBy).descending()
				: Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(categoryService.getAll(pageable));
	}

	@GetMapping("${api.category.get-by-id}")
	public ResponseEntity<Category> getCategoryById(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long id) {

		return ResponseEntity.ok(categoryService.getById(id));
	}

	@PutMapping("${api.category.update-by-id}")
	public ResponseEntity<Category> updateCategory(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long id,
			@Valid @RequestBody CategoryCreateRequest category) {

		return ResponseEntity.ok(categoryService.update(id, category));
	}

	@DeleteMapping("${api.category.delete-by-id}")
	public ResponseEntity<Void> deleteCategory(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long id) {

		categoryService.delete(id);
		return ResponseEntity.noContent().build();
	}

	// MUST be declared before get-by-id to avoid /search matching /{id}
	@GetMapping("${api.category.search}")
	public ResponseEntity<Page<Category>> searchCategories(
			@RequestHeader("X-USER-ID") String email,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) Integer displayOrder,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc")
				? Sort.by(sortBy).descending()
				: Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(categoryService.search(name, displayOrder, pageable));
	}
}