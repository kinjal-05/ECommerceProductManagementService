package servicesImpl;

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
import com.productservice.servicesImpl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Tests")
class ProductServiceImplTest {

	@Mock private ProductRepository productRepository;
	@Mock private CategoryRepository categoryRepository;
	@Mock private ProductEventProducer producer;
	@Mock private StreamBridge streamBridge;

	@InjectMocks
	private ProductServiceImpl productService;

	// ─────────────────────────────────────────────────────────────────────────
	// Helpers
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Category model: id (Long), name (String), displayOrder (Integer)
	 */
	private Category buildCategory(Long id, String name) {
		return Category.builder()
				.id(id)
				.name(name)
				.displayOrder(1)
				.build();
	}

	/**
	 * Product model: id, title, description, author, isbn, price (Double),
	 *                category (Category), images (List<ProductImage>)
	 */
	private Product buildProduct(Long id, String title, String author,
	                             String isbn, Double price, Category category) {
		return Product.builder()
				.id(id)
				.title(title)
				.description("A description")
				.author(author)
				.isbn(isbn)
				.price(price)
				.category(category)
				.build();
	}

	/**
	 * ProductCreateRequest fields: title, description, author, isbn,
	 *                              price (Double), categoryId (Long)
	 */
	private ProductCreateRequest buildRequest(String title, String author,
	                                          String isbn, Double price, Long categoryId) {
		ProductCreateRequest req = new ProductCreateRequest();
		req.setTitle(title);
		req.setDescription("A description");
		req.setAuthor(author);
		req.setIsbn(isbn);
		req.setPrice(price);
		req.setCategoryId(categoryId);
		return req;
	}

	// =========================================================================
	// createProduct
	// =========================================================================

	@Nested
	@DisplayName("createProduct()")
	class CreateProduct {

		@Test
		@DisplayName("Valid request → product saved, ProductEvent published, inventory sync sent")
		void validRequest_savesAndPublishesEvents() {
			Category category = buildCategory(1L, "Science");
			ProductCreateRequest request = buildRequest("Clean Code", "Martin", "ISBN001", 499.0, 1L);
			Product saved = buildProduct(10L, "Clean Code", "Martin", "ISBN001", 499.0, category);

			when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
			when(productRepository.save(any(Product.class))).thenReturn(saved);
			when(streamBridge.send(anyString(), any())).thenReturn(true);

			Product result = productService.createProduct(request);

			assertThat(result.getId()).isEqualTo(10L);
			assertThat(result.getTitle()).isEqualTo("Clean Code");
			assertThat(result.getPrice()).isEqualTo(499.0);

			verify(productRepository).save(any(Product.class));
			verify(producer).sendProductEvent(any(ProductEvent.class));
			verify(streamBridge).send(eq("inventorySync-out-0"), any(ProductEvent1.class));
		}

		@Test
		@DisplayName("ProductEvent published with correct fields")
		void productEvent_hasCorrectFields() {
			Category category = buildCategory(1L, "Fiction");
			ProductCreateRequest request = buildRequest("Dune", "Herbert", "ISBN002", 350.0, 1L);
			Product saved = buildProduct(5L, "Dune", "Herbert", "ISBN002", 350.0, category);

			when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
			when(productRepository.save(any(Product.class))).thenReturn(saved);
			when(streamBridge.send(anyString(), any())).thenReturn(true);

			// ProductEvent fields: productId, title, description, author, isbn,
			//                      price (double), categoryName, createdAt
			ArgumentCaptor<ProductEvent> eventCaptor = ArgumentCaptor.forClass(ProductEvent.class);
			doNothing().when(producer).sendProductEvent(eventCaptor.capture());

			productService.createProduct(request);

			ProductEvent event = eventCaptor.getValue();
			assertThat(event.getTitle()).isEqualTo("Dune");
			assertThat(event.getAuthor()).isEqualTo("Herbert");
			assertThat(event.getIsbn()).isEqualTo("ISBN002");
			assertThat(event.getPrice()).isEqualTo(350.0);
			assertThat(event.getCategoryName()).isEqualTo("Fiction");
		}

		@Test
		@DisplayName("ProductEvent1 sent to inventorySync-out-0 with correct fields")
		void inventoryEvent_hasCorrectFields() {
			Category category = buildCategory(1L, "Tech");
			ProductCreateRequest request = buildRequest("Java", "Gosling", "ISBN003", 600.0, 1L);
			Product saved = buildProduct(7L, "Java", "Gosling", "ISBN003", 600.0, category);

			when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
			when(productRepository.save(any(Product.class))).thenReturn(saved);

			// ProductEvent1 fields: productId (Long), title (String), action (String)
			ArgumentCaptor<ProductEvent1> event1Captor = ArgumentCaptor.forClass(ProductEvent1.class);
			when(streamBridge.send(eq("inventorySync-out-0"), event1Captor.capture())).thenReturn(true);

			productService.createProduct(request);

			ProductEvent1 event1 = event1Captor.getValue();
			assertThat(event1.getProductId()).isEqualTo(7L);
			assertThat(event1.getTitle()).isEqualTo("Java");
			assertThat(event1.getAction()).isEqualTo("CREATED");
		}

		@Test
		@DisplayName("Category not found → ResourceNotFoundException")
		void categoryNotFound_throwsException() {
			when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

			ProductCreateRequest request = buildRequest("Title", "Author", "ISBN", 100.0, 99L);

			assertThatThrownBy(() -> productService.createProduct(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Category not found with id: 99");

			verifyNoInteractions(productRepository, producer);
		}

		@Test
		@DisplayName("StreamBridge failure → handled gracefully, product still returned")
		void streamBridgeFailure_handledGracefully() {
			Category category = buildCategory(1L, "Science");
			ProductCreateRequest request = buildRequest("Book", "Author", "ISBN004", 200.0, 1L);
			Product saved = buildProduct(1L, "Book", "Author", "ISBN004", 200.0, category);

			when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
			when(productRepository.save(any(Product.class))).thenReturn(saved);
			when(streamBridge.send(anyString(), any())).thenThrow(new RuntimeException("Kafka down"));

			// Should NOT throw — StreamBridge failure is caught internally
			assertThatNoException().isThrownBy(() -> productService.createProduct(request));
			verify(productRepository).save(any(Product.class));
		}

		@Test
		@DisplayName("Product built with correct fields from request before saving")
		void productBuilt_withCorrectFields() {
			Category category = buildCategory(2L, "History");
			ProductCreateRequest request = buildRequest("Sapiens", "Harari", "ISBN005", 450.0, 2L);
			Product saved = buildProduct(3L, "Sapiens", "Harari", "ISBN005", 450.0, category);

			when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

			ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
			when(productRepository.save(productCaptor.capture())).thenReturn(saved);
			when(streamBridge.send(anyString(), any())).thenReturn(true);

			productService.createProduct(request);

			Product captured = productCaptor.getValue();
			assertThat(captured.getTitle()).isEqualTo("Sapiens");
			assertThat(captured.getAuthor()).isEqualTo("Harari");
			assertThat(captured.getIsbn()).isEqualTo("ISBN005");
			assertThat(captured.getPrice()).isEqualTo(450.0);
			assertThat(captured.getCategory()).isEqualTo(category);
		}

		@Test
		@DisplayName("createProductFallback → throws RuntimeException with unavailable message")
		void createFallback_throwsRuntimeException() {
			ProductCreateRequest request = buildRequest("X", "Y", "Z", 10.0, 1L);
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> productService.createProductFallback(request, t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Product creation service unavailable");
		}
	}

	// =========================================================================
	// getAllProducts
	// =========================================================================

	@Nested
	@DisplayName("getAllProducts()")
	class GetAllProducts {

		@Test
		@DisplayName("Returns paged Product list")
		void returnsMappedPage() {
			Category category = buildCategory(1L, "Science");
			Product p1 = buildProduct(1L, "Book1", "Author1", "ISBN1", 100.0, category);
			Product p2 = buildProduct(2L, "Book2", "Author2", "ISBN2", 200.0, category);
			Page<Product> page = new PageImpl<>(List.of(p1, p2));
			Pageable pageable = PageRequest.of(0, 10);

			when(productRepository.findAll(pageable)).thenReturn(page);

			Page<Product> result = productService.getAllProducts(pageable);

			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent().get(0).getTitle()).isEqualTo("Book1");
			assertThat(result.getContent().get(1).getTitle()).isEqualTo("Book2");
		}

		@Test
		@DisplayName("Empty repository → returns empty page")
		void emptyRepository_returnsEmptyPage() {
			Page<Product> emptyPage = new PageImpl<>(List.of());
			Pageable pageable = PageRequest.of(0, 10);

			when(productRepository.findAll(pageable)).thenReturn(emptyPage);

			Page<Product> result = productService.getAllProducts(pageable);

			assertThat(result.getContent()).isEmpty();
		}

		@Test
		@DisplayName("getAllProductsFallback (Throwable) → throws RuntimeException")
		void getAllFallback_throwable_throwsRuntimeException() {
			Pageable pageable = PageRequest.of(0, 10);
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> productService.getAllProductsFallback(pageable, t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Unable to fetch products");
		}

		@Test
		@DisplayName("getAllProductsFallback (Exception) → returns empty page")
		void getAllFallback_exception_returnsEmptyPage() {
			Pageable pageable = PageRequest.of(0, 10);
			Exception ex = new RuntimeException("CB triggered");

			Page<Product> result = productService.getAllProductsFallback(pageable, ex);

			assertThat(result.getContent()).isEmpty();
		}
	}

	// =========================================================================
	// updateProduct
	// =========================================================================

	@Nested
	@DisplayName("updateProduct()")
	class UpdateProduct {

		@Test
		@DisplayName("Valid update → product updated with new fields and saved")
		void validUpdate_returnsUpdatedProduct() {
			Category oldCat = buildCategory(1L, "OldCat");
			Category newCat = buildCategory(2L, "NewCat");
			Product existing = buildProduct(1L, "OldTitle", "OldAuthor", "ISBN1", 100.0, oldCat);
			ProductCreateRequest request = buildRequest("NewTitle", "NewAuthor", "ISBN2", 200.0, 2L);
			Product updated = buildProduct(1L, "NewTitle", "NewAuthor", "ISBN2", 200.0, newCat);

			when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
			when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCat));
			when(productRepository.save(any(Product.class))).thenReturn(updated);

			Product result = productService.updateProduct(1L, request);

			assertThat(result.getTitle()).isEqualTo("NewTitle");
			assertThat(result.getAuthor()).isEqualTo("NewAuthor");
			assertThat(result.getPrice()).isEqualTo(200.0);
			assertThat(result.getCategory().getName()).isEqualTo("NewCat");
		}

		@Test
		@DisplayName("toBuilder preserves product id after update")
		void toBuilder_preservesId() {
			Category cat = buildCategory(1L, "Cat");
			Product existing = buildProduct(10L, "OldTitle", "OldAuthor", "ISBN", 100.0, cat);
			ProductCreateRequest request = buildRequest("NewTitle", "NewAuthor", "ISBN2", 150.0, 1L);

			when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
			when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

			ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
			when(productRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

			productService.updateProduct(10L, request);

			assertThat(captor.getValue().getId()).isEqualTo(10L);
			assertThat(captor.getValue().getTitle()).isEqualTo("NewTitle");
		}

		@Test
		@DisplayName("Product not found → ResourceNotFoundException")
		void productNotFound_throwsException() {
			when(productRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> productService.updateProduct(99L,
					buildRequest("T", "A", "I", 10.0, 1L)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Product not found with id: 99");
		}

		@Test
		@DisplayName("Category not found → ResourceNotFoundException")
		void categoryNotFound_throwsException() {
			Category cat = buildCategory(1L, "Cat");
			Product existing = buildProduct(1L, "Title", "Author", "ISBN", 100.0, cat);

			when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
			when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> productService.updateProduct(1L,
					buildRequest("T", "A", "I", 10.0, 99L)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Category not found with id: 99");
		}

		@Test
		@DisplayName("updateProductFallback → throws RuntimeException with unavailable message")
		void updateFallback_throwsRuntimeException() {
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> productService.updateProductFallback(1L,
					buildRequest("T", "A", "I", 10.0, 1L), t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Product update service unavailable");
		}
	}

	// =========================================================================
	// deleteProduct
	// =========================================================================

	@Nested
	@DisplayName("deleteProduct()")
	class DeleteProduct {

		@Test
		@DisplayName("Existing product → deleted by id")
		void existingProduct_deleted() {
			when(productRepository.existsById(1L)).thenReturn(true);

			productService.deleteProduct(1L);

			verify(productRepository).deleteById(1L);
		}

		@Test
		@DisplayName("Non-existent product → ResourceNotFoundException, no deleteById called")
		void notFound_throwsException() {
			when(productRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> productService.deleteProduct(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Product not found with id: 99");

			verify(productRepository, never()).deleteById(any());
		}

		@Test
		@DisplayName("deleteProductFallback → throws RuntimeException with unavailable message")
		void deleteFallback_throwsRuntimeException() {
			Exception ex = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> productService.deleteProductFallback(1L, ex))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Product delete service unavailable");
		}
	}

	// =========================================================================
	// searchProducts
	// =========================================================================

	@Nested
	@DisplayName("searchProducts()")
	class SearchProducts {

		@Test
		@DisplayName("Matching products → returns filtered page")
		void matchingProducts_returnsPage() {
			Category cat = buildCategory(1L, "Science");
			Product p = buildProduct(1L, "Physics", "Newton", "ISBN1", 300.0, cat);
			Page<Product> page = new PageImpl<>(List.of(p));
			Pageable pageable = PageRequest.of(0, 10);

			when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

			Page<Product> result = productService.searchProducts(
					"Physics", null, null, null, null, null, pageable);

			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).getTitle()).isEqualTo("Physics");
		}

		@Test
		@DisplayName("No products found → ResourceNotFoundException")
		void noProducts_throwsException() {
			Page<Product> emptyPage = new PageImpl<>(List.of());
			Pageable pageable = PageRequest.of(0, 10);

			when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

			assertThatThrownBy(() -> productService.searchProducts(
					"NonExistent", null, null, null, null, null, pageable))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("No products found with given search criteria");
		}

		@Test
		@DisplayName("minPrice > maxPrice → BadRequestException")
		void minPriceGreaterThanMaxPrice_throwsBadRequest() {
			Pageable pageable = PageRequest.of(0, 10);

			assertThatThrownBy(() -> productService.searchProducts(
					null, null, null, null, 500.0, 100.0, pageable))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("minPrice cannot be greater than maxPrice");

			verifyNoInteractions(productRepository);
		}

		@Test
		@DisplayName("minPrice equals maxPrice → no exception, search proceeds")
		void minPriceEqualsMaxPrice_searchProceeds() {
			Category cat = buildCategory(1L, "Cat");
			Product p = buildProduct(1L, "Book", "Author", "ISBN", 200.0, cat);
			Page<Product> page = new PageImpl<>(List.of(p));
			Pageable pageable = PageRequest.of(0, 10);

			when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

			assertThatNoException().isThrownBy(() ->
					productService.searchProducts(null, null, null, null, 200.0, 200.0, pageable));
		}

		@Test
		@DisplayName("All filters null → returns all matching products")
		void allFiltersNull_returnsAll() {
			Category cat = buildCategory(1L, "Cat");
			Page<Product> page = new PageImpl<>(List.of(
					buildProduct(1L, "B1", "A1", "I1", 100.0, cat),
					buildProduct(2L, "B2", "A2", "I2", 200.0, cat)
			));
			Pageable pageable = PageRequest.of(0, 10);

			when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

			Page<Product> result = productService.searchProducts(
					null, null, null, null, null, null, pageable);

			assertThat(result.getContent()).hasSize(2);
		}

		@Test
		@DisplayName("searchProductsFallback → returns page with fallback product")
		void searchFallback_returnsFallbackPage() {
			Pageable pageable = PageRequest.of(0, 10);
			Exception ex = new RuntimeException("CB triggered");

			Page<Product> result = productService.searchProductsFallback(
					null, null, null, null, null, null, pageable, ex);

			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).getTitle())
					.isEqualTo("Service temporarily unavailable");
			assertThat(result.getContent().get(0).getAuthor()).isEqualTo("System");
			assertThat(result.getContent().get(0).getPrice()).isEqualTo(0.0);
		}
	}

	// =========================================================================
	// getProductById
	// =========================================================================

	@Nested
	@DisplayName("getProductById()")
	class GetProductById {

		@Test
		@DisplayName("Existing product → returns Product")
		void existingProduct_returnsProduct() {
			Category cat = buildCategory(1L, "Cat");
			Product product = buildProduct(1L, "Dune", "Herbert", "ISBN1", 400.0, cat);
			when(productRepository.findById(1L)).thenReturn(Optional.of(product));

			Product result = productService.getProductById(1L);

			assertThat(result.getId()).isEqualTo(1L);
			assertThat(result.getTitle()).isEqualTo("Dune");
			assertThat(result.getAuthor()).isEqualTo("Herbert");
			assertThat(result.getPrice()).isEqualTo(400.0);
		}

		@Test
		@DisplayName("Non-existent product → ResourceNotFoundException")
		void notFound_throwsException() {
			when(productRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> productService.getProductById(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Product not found with id: 99");
		}

		@Test
		@DisplayName("getProductByIdFallback → returns fallback Product with correct id")
		void getByIdFallback_returnsFallbackProduct() {
			Exception ex = new RuntimeException("CB triggered");

			Product result = productService.getProductByIdFallback(42L, ex);

			assertThat(result.getId()).isEqualTo(42L);
			assertThat(result.getTitle()).isEqualTo("Product temporarily unavailable");
			assertThat(result.getPrice()).isEqualTo(0.0);
		}
	}
}
