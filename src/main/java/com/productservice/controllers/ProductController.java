package com.productservice.controllers;

import com.productservice.dtos.ProductCreateRequest;
import com.productservice.models.Product;
import com.productservice.services.ProductService;
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
 * api.product.base         → /api/products
 * api.product.create       → /add
 * api.product.get-all      → /getAll
 * api.product.get-by-id    → /{id}
 * api.product.update-by-id → /{id}
 * api.product.delete-by-id → /{id}
 * api.product.search       → /search
 *
 * Bugs fixed from original:
 * - @RequestMapping("/api/products/") trailing slash removed → causes double slash issues
 * - @RequestHeader + @RequestBody on same param → separated correctly
 * - @RequestHeader + @PathVariable on same param → separated correctly
 * - @RequestHeader + @RequestParam on same param → removed header from pageable params
 * - sort param split logic kept as-is (valid pattern), but sortDir approach standardized
 */
@RestController
@RequestMapping("${api.product.base}")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@PostMapping("${api.product.create}")
	public ResponseEntity<Product> createProduct(
			@RequestHeader("X-USER-ID") String email,
			@Valid @RequestBody ProductCreateRequest product) {

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(productService.createProduct(product));
	}

	@GetMapping("${api.product.get-all}")
	public ResponseEntity<Page<Product>> getAllProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc")
				? Sort.by(sortBy).descending()
				: Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(productService.getAllProducts(pageable));
	}

	@PutMapping("${api.product.update-by-id}")
	public ResponseEntity<Product> updateProduct(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long id,
			@Valid @RequestBody ProductCreateRequest product) {

		return ResponseEntity.ok(productService.updateProduct(id, product));
	}

	@DeleteMapping("${api.product.delete-by-id}")
	public ResponseEntity<Void> deleteProduct(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long id) {

		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}

	// MUST be declared before get-by-id to avoid /search matching /{id}
	@GetMapping("${api.product.search}")
	public ResponseEntity<Page<Product>> searchProducts(
			@RequestHeader("X-USER-ID") String email,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String author,
			@RequestParam(required = false) String isbn,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) Double minPrice,
			@RequestParam(required = false) Double maxPrice,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc")
				? Sort.by(sortBy).descending()
				: Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(
				productService.searchProducts(title, author, isbn, categoryId, minPrice, maxPrice, pageable)
		);
	}

	@GetMapping("${api.product.get-by-id}")
	public ResponseEntity<Product> getProductById(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long id) {

		return ResponseEntity.ok(productService.getProductById(id));
	}
}