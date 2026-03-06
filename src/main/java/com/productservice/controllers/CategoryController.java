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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories/v1")
@RequiredArgsConstructor
public class CategoryController {
	private final CategoryService categoryService;

	@PostMapping("/creatCategory")
	public ResponseEntity<?> createCategory(@RequestHeader("X-USER-ID") String email,
	                                        @Valid @RequestBody CategoryCreateRequest category) {
		System.out.println(email);
		Category savedCategory = categoryService.create(category);
		return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
	}

	@GetMapping("/getAllCategories")
	public ResponseEntity<?> getAllCategories(@RequestHeader("X-USER-ID") @RequestParam(defaultValue = "0") int page,
	                                          @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy,
	                                          @RequestParam(defaultValue = "asc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);

		return ResponseEntity.ok(categoryService.getAll(pageable));
	}

	@GetMapping("/getById/{id}")
	public ResponseEntity<Category> getCategoryById(@RequestHeader("X-USER-ID") @PathVariable Long id) {
		return ResponseEntity.ok(categoryService.getById(id));
	}

	@PutMapping("/updateById/{id}")
	public ResponseEntity<Category> updateCategory(@RequestHeader("X-USER-ID") @PathVariable Long id,
	                                               @Valid @RequestBody CategoryCreateRequest category) {
		return ResponseEntity.ok(categoryService.update(id, category));
	}

	@DeleteMapping("/deleteById/{id}")
	public ResponseEntity<Void> deleteCategory(@RequestHeader("X-USER-ID") @PathVariable Long id) {
		categoryService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public ResponseEntity<Page<Category>> searchCategories(
			@RequestHeader("X-USER-ID") @RequestParam(required = false) String name,
			@RequestParam(required = false) Integer displayOrder, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);

		return ResponseEntity.ok(categoryService.search(name, displayOrder, pageable));
	}

}

