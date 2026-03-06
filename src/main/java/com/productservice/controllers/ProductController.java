package com.productservice.controllers;
import com.productservice.dtos.ProductCreateRequest;
import com.productservice.models.Product;
import com.productservice.services.ProductService;
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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products/")
@RequiredArgsConstructor
public class ProductController {
	private final ProductService productService;

	@PostMapping("/addProducts")
	public ResponseEntity<Product> createProduct(
			@RequestHeader("X-USER-ID") @Valid @RequestBody ProductCreateRequest product) {
		return new ResponseEntity<>(productService.createProduct(product), HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<Page<Product>> getAllProducts(
			@RequestHeader("X-USER-ID") @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size, @RequestParam(defaultValue = "id,asc") String sort) {
		String[] sortParams = sort.split(",");
		Pageable pageable = PageRequest.of(page, size,
				Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]));

		return ResponseEntity.ok(productService.getAllProducts(pageable));
	}

	@PutMapping("/updateById/{id}")
	public ResponseEntity<Product> updateProduct(@RequestHeader("X-USER-ID") @PathVariable Long id,
	                                             @RequestBody ProductCreateRequest product) {
		return ResponseEntity.ok(productService.updateProduct(id, product));
	}

	@DeleteMapping("/deleteById/{id}")
	public ResponseEntity<Void> deleteProduct(@RequestHeader("X-USER-ID") @PathVariable Long id) {
		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public ResponseEntity<Page<Product>> searchProducts(
			@RequestHeader("X-USER-ID") @RequestParam(required = false) String title,
			@RequestParam(required = false) String author, @RequestParam(required = false) String isbn,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) Double minPrice,
			@RequestParam(required = false) Double maxPrice, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {

		Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);

		return ResponseEntity
				.ok(productService.searchProducts(title, author, isbn, categoryId, minPrice, maxPrice, pageable));
	}

	@GetMapping("/getProductById/{id}")
	public ResponseEntity<Product> getProductById(@RequestHeader("X-USER-ID") @PathVariable Long id) {
		Product product = productService.getProductById(id); // call service method
		return ResponseEntity.ok(product);
	}
}

