package servicesImpl;



import com.productservice.commondtos.CategoryEvent;
import com.productservice.dtos.CategoryCreateRequest;
import com.productservice.exceptions.ResourceNotFoundException;
import com.productservice.models.Category;
import com.productservice.publishEvent.CategoryEventProducer;
import com.productservice.repositories.CategoryRepository;
import com.productservice.servicesImpl.CategoryServiceImpl;
import com.productservice.specifications.CategorySpecifications;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
@DisplayName("CategoryServiceImpl Tests")
class CategoryServiceImplTest {

	@Mock private CategoryRepository categoryRepository;
	@Mock private CategoryEventProducer producer;

	@InjectMocks
	private CategoryServiceImpl categoryService;

	// ─────────────────────────────────────────────────────────────────────────
	// Helpers
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Category model fields: id (Long), name (String), displayOrder (Integer), products (List)
	 * Built using @Builder(toBuilder = true)
	 */
	private Category buildCategory(Long id, String name, Integer displayOrder) {
		return Category.builder()
				.id(id)
				.name(name)
				.displayOrder(displayOrder)
				.build();
	}

	/**
	 * CategoryCreateRequest fields: name (String), displayOrder (Integer)
	 */
	private CategoryCreateRequest buildRequest(String name, Integer displayOrder) {
		CategoryCreateRequest req = new CategoryCreateRequest();
		req.setName(name);
		req.setDisplayOrder(displayOrder);
		return req;
	}

	// =========================================================================
	// create
	// =========================================================================

	@Nested
	@DisplayName("create()")
	class Create {

		@Test
		@DisplayName("Valid request → CategoryEvent published and category saved")
		void validRequest_savesAndPublishesEvent() {
			CategoryCreateRequest request = buildRequest("Science", 1);
			Category saved = buildCategory(1L, "Science", 1);

			when(categoryRepository.save(any(Category.class))).thenReturn(saved);

			Category result = categoryService.create(request);

			// Category saved with correct fields
			assertThat(result.getId()).isEqualTo(1L);
			assertThat(result.getName()).isEqualTo("Science");
			assertThat(result.getDisplayOrder()).isEqualTo(1);

			verify(categoryRepository).save(any(Category.class));
		}

		@Test
		@DisplayName("CategoryEvent sent with correct name")
		void categoryEvent_hasCorrectName() {
			CategoryCreateRequest request = buildRequest("Fiction", 2);
			Category saved = buildCategory(1L, "Fiction", 2);

			when(categoryRepository.save(any(Category.class))).thenReturn(saved);

			// CategoryEvent fields: categoryId (Long), name (String)
			ArgumentCaptor<CategoryEvent> eventCaptor = ArgumentCaptor.forClass(CategoryEvent.class);
			doNothing().when(producer).sendCategoryEvent(eventCaptor.capture());

			categoryService.create(request);

			CategoryEvent event = eventCaptor.getValue();
			assertThat(event.getName()).isEqualTo("Fiction");
		}

		@Test
		@DisplayName("Category built with correct name and displayOrder before saving")
		void categoryBuilt_withCorrectFields() {
			CategoryCreateRequest request = buildRequest("History", 5);
			Category saved = buildCategory(1L, "History", 5);

			ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
			when(categoryRepository.save(categoryCaptor.capture())).thenReturn(saved);

			categoryService.create(request);

			Category captured = categoryCaptor.getValue();
			assertThat(captured.getName()).isEqualTo("History");
			assertThat(captured.getDisplayOrder()).isEqualTo(5);
		}

		@Test
		@DisplayName("DataIntegrityViolationException → RuntimeException with constraint message")
		void dataIntegrityViolation_throwsRuntimeException() {
			CategoryCreateRequest request = buildRequest("Duplicate", 1);

			when(categoryRepository.save(any(Category.class)))
					.thenThrow(new DataIntegrityViolationException("constraint"));

			assertThatThrownBy(() -> categoryService.create(request))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Failed to create category due to database constraint violation");
		}

		@Test
		@DisplayName("Unexpected exception → RuntimeException with unexpected error message")
		void unexpectedException_throwsRuntimeException() {
			CategoryCreateRequest request = buildRequest("Error", 1);

			when(categoryRepository.save(any(Category.class)))
					.thenThrow(new RuntimeException("DB down"));

			assertThatThrownBy(() -> categoryService.create(request))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Unexpected error while creating category");
		}

		@Test
		@DisplayName("createFallback → throws RuntimeException with unavailable message")
		void createFallback_throwsRuntimeException() {
			CategoryCreateRequest request = buildRequest("Test", 1);
			Exception ex = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> categoryService.createFallback(request, ex))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Category service is currently unavailable");
		}
	}

	// =========================================================================
	// getAll
	// =========================================================================

	@Nested
	@DisplayName("getAll()")
	class GetAll {

		@Test
		@DisplayName("Returns paged Category list")
		void returnsMappedPage() {
			Category c1 = buildCategory(1L, "Science", 1);
			Category c2 = buildCategory(2L, "Fiction", 2);
			Page<Category> page = new PageImpl<>(List.of(c1, c2));
			Pageable pageable = PageRequest.of(0, 10);

			when(categoryRepository.findAll(pageable)).thenReturn(page);

			Page<Category> result = categoryService.getAll(pageable);

			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getContent().get(0).getName()).isEqualTo("Science");
			assertThat(result.getContent().get(1).getName()).isEqualTo("Fiction");
		}

		@Test
		@DisplayName("Empty repository → returns empty page")
		void emptyRepository_returnsEmptyPage() {
			Page<Category> emptyPage = new PageImpl<>(List.of());
			Pageable pageable = PageRequest.of(0, 10);

			when(categoryRepository.findAll(pageable)).thenReturn(emptyPage);

			Page<Category> result = categoryService.getAll(pageable);

			assertThat(result.getContent()).isEmpty();
		}

		@Test
		@DisplayName("Repository throws exception → RuntimeException with fetch message")
		void repositoryThrows_runtimeException() {
			Pageable pageable = PageRequest.of(0, 10);

			when(categoryRepository.findAll(pageable))
					.thenThrow(new RuntimeException("DB error"));

			assertThatThrownBy(() -> categoryService.getAll(pageable))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Failed to fetch categories");
		}

		@Test
		@DisplayName("getAllFallback → throws RuntimeException")
		void getAllFallback_throwsRuntimeException() {
			Pageable pageable = PageRequest.of(0, 10);
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> categoryService.getAllFallback(pageable, t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Fetch categories service unavailable");
		}
	}

	// =========================================================================
	// getById
	// =========================================================================

	@Nested
	@DisplayName("getById()")
	class GetById {

		@Test
		@DisplayName("Existing id → returns Category")
		void existingId_returnsCategory() {
			Category category = buildCategory(1L, "Science", 3);
			when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

			Category result = categoryService.getById(1L);

			assertThat(result.getId()).isEqualTo(1L);
			assertThat(result.getName()).isEqualTo("Science");
			assertThat(result.getDisplayOrder()).isEqualTo(3);
		}

		@Test
		@DisplayName("Non-existent id → ResourceNotFoundException")
		void notFound_throwsException() {
			when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> categoryService.getById(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Category not found with id: 99");
		}

		@Test
		@DisplayName("getByIdFallback → throws RuntimeException with id in message")
		void getByIdFallback_throwsRuntimeException() {
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> categoryService.getByIdFallback(5L, t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("5");
		}
	}

	// =========================================================================
	// update
	// =========================================================================

	@Nested
	@DisplayName("update()")
	class Update {

		@Test
		@DisplayName("Existing category → updated with new name and displayOrder")
		void validUpdate_returnsUpdatedCategory() {
			Category existing = buildCategory(1L, "OldName", 1);
			Category updated  = buildCategory(1L, "NewName", 2);
			CategoryCreateRequest request = buildRequest("NewName", 2);

			when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
			when(categoryRepository.save(any(Category.class))).thenReturn(updated);

			Category result = categoryService.update(1L, request);

			assertThat(result.getName()).isEqualTo("NewName");
			assertThat(result.getDisplayOrder()).isEqualTo(2);
			verify(categoryRepository).save(any(Category.class));
		}

		@Test
		@DisplayName("toBuilder used → id preserved after update")
		void toBuilder_preservesId() {
			Category existing = buildCategory(10L, "OldName", 1);
			CategoryCreateRequest request = buildRequest("NewName", 5);

			when(categoryRepository.findById(10L)).thenReturn(Optional.of(existing));

			ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
			when(categoryRepository.save(captor.capture()))
					.thenAnswer(inv -> inv.getArgument(0));

			categoryService.update(10L, request);

			// id must be preserved from existing category via toBuilder()
			assertThat(captor.getValue().getId()).isEqualTo(10L);
			assertThat(captor.getValue().getName()).isEqualTo("NewName");
			assertThat(captor.getValue().getDisplayOrder()).isEqualTo(5);
		}

		@Test
		@DisplayName("Non-existent id → ResourceNotFoundException")
		void notFound_throwsException() {
			when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> categoryService.update(99L, buildRequest("X", 1)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Category not found with id: 99");
		}

		@Test
		@DisplayName("updateFallback → throws RuntimeException with id in message")
		void updateFallback_throwsRuntimeException() {
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> categoryService.updateFallback(7L, buildRequest("X", 1), t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("7");
		}
	}

	// =========================================================================
	// delete
	// =========================================================================

	@Nested
	@DisplayName("delete()")
	class Delete {

		@Test
		@DisplayName("Existing category → deleted successfully")
		void existingCategory_deleted() {
			Category category = buildCategory(1L, "Science", 1);
			when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

			categoryService.delete(1L);

			verify(categoryRepository).delete((Category) category);
		}

		@Test
		@DisplayName("Non-existent id → ResourceNotFoundException")
		void notFound_throwsException() {
			when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> categoryService.delete(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Category not found with id: 99");

			verify(categoryRepository, never()).delete((Category) any());
		}

		@Test
		@DisplayName("deleteFallback → throws RuntimeException with id in message")
		void deleteFallback_throwsRuntimeException() {
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> categoryService.deleteFallback(3L, t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("3");
		}
	}

	// =========================================================================
	// search
	// =========================================================================

	@Nested
	@DisplayName("search()")
	class Search {

		@Test
		@DisplayName("Matching results → returns filtered page")
		void matchingResults_returnsPage() {
			Category c = buildCategory(1L, "Science", 1);
			Page<Category> page = new PageImpl<>(List.of(c));
			Pageable pageable = PageRequest.of(0, 10);

			when(categoryRepository.findAll(any(Specification.class), eq(pageable)))
					.thenReturn(page);

			Page<Category> result = categoryService.search("Science", null, pageable);

			assertThat(result.getContent()).hasSize(1);
			assertThat(result.getContent().get(0).getName()).isEqualTo("Science");
		}

		@Test
		@DisplayName("No matching results → returns empty page")
		void noResults_returnsEmptyPage() {
			Page<Category> emptyPage = new PageImpl<>(List.of());
			Pageable pageable = PageRequest.of(0, 10);

			when(categoryRepository.findAll(any(Specification.class), eq(pageable)))
					.thenReturn(emptyPage);

			Page<Category> result = categoryService.search("NonExistent", 99, pageable);

			assertThat(result.getContent()).isEmpty();
		}

		@Test
		@DisplayName("Null name and displayOrder → returns all categories")
		void nullFilters_returnsAll() {
			Category c1 = buildCategory(1L, "Science", 1);
			Category c2 = buildCategory(2L, "Fiction", 2);
			Page<Category> page = new PageImpl<>(List.of(c1, c2));
			Pageable pageable = PageRequest.of(0, 10);

			when(categoryRepository.findAll(any(Specification.class), eq(pageable)))
					.thenReturn(page);

			Page<Category> result = categoryService.search(null, null, pageable);

			assertThat(result.getContent()).hasSize(2);
		}

		@Test
		@DisplayName("searchFallback → throws RuntimeException")
		void searchFallback_throwsRuntimeException() {
			Pageable pageable = PageRequest.of(0, 10);
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> categoryService.searchFallback("Science", 1, pageable, t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Search category service unavailable");
		}
	}
}