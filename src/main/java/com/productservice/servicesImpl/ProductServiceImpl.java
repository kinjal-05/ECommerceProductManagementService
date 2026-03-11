package com.productservice.servicesImpl;

import com.productservice.commondtos.ProductEvent;
import com.productservice.commondtos.ProductEvent1;
import com.productservice.dtos.ProductCreateRequest;
import com.productservice.exceptions.BadRequestException;
import com.productservice.exceptions.ResourceNotFoundException;
import com.productservice.models.Category;
import com.productservice.models.Product;
import com.productservice.publishEvent.ProductEventProducer;
import com.productservice.repositories.CategoryRepository;
import com.productservice.repositories.ProductRepository;
import com.productservice.services.ProductService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.productservice.specifications.ProductSpecification.*;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductEventProducer producer;
	private final StreamBridge streamBridge;

	@Override
	@CircuitBreaker(name = "productServiceCB", fallbackMethod = "createProductFallback")
	public Product createProduct(ProductCreateRequest product) {
		Category category = categoryRepository.findById(product.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with id: " + product.getCategoryId()));
		Product p = Product.builder().title(product.getTitle()).description(product.getDescription())
				.author(product.getAuthor()).isbn(product.getIsbn()).price(product.getPrice()).category(category)
				.build();
		Product saved = productRepository.save(p);
		ProductEvent event = ProductEvent.builder().title(saved.getTitle()).description(saved.getDescription())
				.author(saved.getAuthor()).isbn(saved.getIsbn()).price(saved.getPrice())
				.categoryName(category.getName()).build();
		producer.sendProductEvent(event);
		try {
			ProductEvent1 event1 = ProductEvent1.builder().productId(saved.getId()).title(saved.getTitle())
					.action("CREATED").build();
			streamBridge.send("inventorySync-out-0", event1);
		} catch (Exception e) {
			System.out.println("❌ Failed to publish inventory event: " + e.getMessage());
		}
		return saved;
	}

	public Product createProductFallback(ProductCreateRequest product, Throwable t) {
		throw new RuntimeException("Product creation service unavailable. Please try again later.", t);
	}

	@Override
	@CircuitBreaker(name = "productServiceCB", fallbackMethod = "getAllProductsFallback")
	public Page<Product> getAllProducts(Pageable pageable) {
		return productRepository.findAll(pageable);
	}

	public Page<Product> getAllProductsFallback(Pageable pageable, Throwable t) {
		throw new RuntimeException("Unable to fetch products. Please try again later.", t);
	}

	public Page<Product> getAllProductsFallback(Pageable pageable, Exception ex) {

		System.out.println("Fallback triggered: " + ex.getMessage());

		return Page.empty(pageable);
	}

	@Override
	@CircuitBreaker(name = "productServiceCB", fallbackMethod = "updateProductFallback")
	public Product updateProduct(Long id, ProductCreateRequest product) {
		Product p = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
		Category c = categoryRepository.findById(product.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with id: " + product.getCategoryId()));
		Product updatedProduct = p.toBuilder().title(product.getTitle()).author(product.getAuthor())
				.description(product.getDescription()).isbn(product.getIsbn()).price(product.getPrice()).category(c)
				.build();
		return productRepository.save(updatedProduct);
	}

	public Product updateProductFallback(Long id, ProductCreateRequest product, Throwable t) {
		throw new RuntimeException("Product update service unavailable. Please try again later.", t);
	}

	@Override
	@CircuitBreaker(name = "productServiceCB", fallbackMethod = "deleteProductFallback")
	public void deleteProduct(Long id) {
		if (!productRepository.existsById(id)) {
			throw new ResourceNotFoundException("Product not found with id: " + id);
		}
		productRepository.deleteById(id);
	}

	public void deleteProductFallback(Long id, Exception ex) {

		System.out.println("Circuit Breaker triggered for deleteProduct: " + ex.getMessage());

		throw new RuntimeException("Product delete service unavailable. Please try again later.");
	}

	@Override
	@CircuitBreaker(name = "productServiceCB", fallbackMethod = "searchProductsFallback")
	public Page<Product> searchProducts(String title, String author, String isbn, Long categoryId, Double minPrice,
	                                    Double maxPrice, Pageable pageable) {
		if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
			throw new BadRequestException("minPrice cannot be greater than maxPrice");
		}
		Specification<Product> spec = Specification.where(hasTitle(title)).and(hasAuthor(author)).and(hasIsbn(isbn))
				.and(hasCategory(categoryId)).and(priceGreaterThanOrEqual(minPrice))
				.and(priceLessThanOrEqual(maxPrice));
		Page<Product> products = productRepository.findAll(spec, pageable);
		if (products.isEmpty()) {
			throw new ResourceNotFoundException("No products found with given search criteria");
		}
		return products;
	}

	public Page<Product> searchProductsFallback(String title,
	                                            String author,
	                                            String isbn,
	                                            Long categoryId,
	                                            Double minPrice,
	                                            Double maxPrice,
	                                            Pageable pageable,
	                                            Exception ex) {

		System.out.println("Fallback triggered: " + ex.getMessage());

		List<Product> fallbackProducts = List.of(
				Product.builder()
						.id(0L)
						.title("Service temporarily unavailable")
						.author("System")
						.price(0.0)
						.build()
		);

		return new PageImpl<>(fallbackProducts, pageable, fallbackProducts.size());
	}

	@Override
	@CircuitBreaker(name = "productServiceCB", fallbackMethod = "getProductByIdFallback")
	public Product getProductById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
	}

	public Product getProductByIdFallback(Long id, Exception ex) {

		System.out.println("Circuit Breaker triggered for Product Service: " + ex.getMessage());

		return Product.builder()
				.id(id)
				.title("Product temporarily unavailable")
				.description("Service unavailable. Please try again later.")
				.price(0.0)
				.build();
	}
}
