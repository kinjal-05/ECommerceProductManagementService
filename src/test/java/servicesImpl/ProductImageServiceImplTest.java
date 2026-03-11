package servicesImpl;

import com.productservice.models.Product;
import com.productservice.models.ProductImage;
import com.productservice.repositories.ProductImageRepository;
import com.productservice.repositories.ProductRepository;
import com.productservice.servicesImpl.ProductImageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductImageServiceImpl Tests")
class ProductImageServiceImplTest {

	@Mock private ProductRepository productRepository;
	@Mock private ProductImageRepository imageRepository;

	@InjectMocks
	private ProductImageServiceImpl productImageService;

	// ─────────────────────────────────────────────────────────────────────────
	// Helpers
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Product model fields (from earlier): id, name, price, etc.
	 * Only id is needed here since ProductImage holds a reference.
	 */
	private Product buildProduct(Long id) {
		return Product.builder()
				.id(id)
				.build();
	}

	/**
	 * ProductImage fields: id (Long), fileName (String), product (Product)
	 */
	private ProductImage buildProductImage(Long id, String fileName, Product product) {
		return ProductImage.builder()
				.id(id)
				.fileName(fileName)
				.product(product)
				.build();
	}

	/**
	 * Mock MultipartFile with given original filename and bytes.
	 */
	private MultipartFile mockMultipartFile(String originalFilename, byte[] bytes) throws IOException {
		MultipartFile file = mock(MultipartFile.class);
		when(file.getOriginalFilename()).thenReturn(originalFilename);
		when(file.getBytes()).thenReturn(bytes);
		return file;
	}

	// =========================================================================
	// uploadProductImage
	// =========================================================================

	@Nested
	@DisplayName("uploadProductImage()")
	class UploadProductImage {

		@Test
		@DisplayName("Valid product and image → ProductImage saved with correct fileName and product")
		void validUpload_savesProductImage() throws IOException {
			MultipartFile image = mockMultipartFile("test.jpg", "image-bytes".getBytes());
			Product product = buildProduct(1L);

			when(productRepository.findById(1L)).thenReturn(Optional.of(product));

			try (MockedStatic<Files> filesMock = mockStatic(Files.class);
			     MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {

				Path mockDir  = mock(Path.class);
				Path mockFile = mock(Path.class);

				pathsMock.when(() -> Paths.get("uploads/products/")).thenReturn(mockDir);
				pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockFile);

				filesMock.when(() -> Files.createDirectories(mockDir)).thenReturn(mockDir);
				filesMock.when(() -> Files.write(eq(mockFile), any(byte[].class))).thenReturn(mockFile);

				productImageService.uploadProductImage(1L, image);
			}

			// ProductImage saved with product set
			ArgumentCaptor<ProductImage> captor = ArgumentCaptor.forClass(ProductImage.class);
			verify(imageRepository).save(captor.capture());

			ProductImage saved = captor.getValue();
			assertThat(saved.getProduct()).isEqualTo(product);
			assertThat(saved.getFileName()).endsWith("_test.jpg");
		}

		@Test
		@DisplayName("Product not found → RuntimeException")
		void productNotFound_throwsException() throws IOException {
			MultipartFile image = mockMultipartFile("test.jpg", "bytes".getBytes());

			when(productRepository.findById(99L)).thenReturn(Optional.empty());

			try (MockedStatic<Files> filesMock = mockStatic(Files.class);
			     MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {

				Path mockDir  = mock(Path.class);
				Path mockFile = mock(Path.class);

				pathsMock.when(() -> Paths.get("uploads/products/")).thenReturn(mockDir);
				pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockFile);

				filesMock.when(() -> Files.createDirectories(mockDir)).thenReturn(mockDir);
				filesMock.when(() -> Files.write(eq(mockFile), any(byte[].class))).thenReturn(mockFile);

				assertThatThrownBy(() -> productImageService.uploadProductImage(99L, image))
						.isInstanceOf(RuntimeException.class)
						.hasMessageContaining("Product not found");
			}

			verify(imageRepository, never()).save(any());
		}


		@Test
		@DisplayName("Saved fileName contains the original filename as suffix")
		void savedFileName_containsOriginalFilename() throws IOException {
			MultipartFile image = mockMultipartFile("photo.png", "data".getBytes());
			Product product = buildProduct(2L);

			when(productRepository.findById(2L)).thenReturn(Optional.of(product));

			try (MockedStatic<Files> filesMock = mockStatic(Files.class);
			     MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {

				Path mockDir  = mock(Path.class);
				Path mockFile = mock(Path.class);

				pathsMock.when(() -> Paths.get("uploads/products/")).thenReturn(mockDir);
				pathsMock.when(() -> Paths.get(anyString())).thenReturn(mockFile);

				filesMock.when(() -> Files.createDirectories(mockDir)).thenReturn(mockDir);
				filesMock.when(() -> Files.write(eq(mockFile), any(byte[].class))).thenReturn(mockFile);

				productImageService.uploadProductImage(2L, image);
			}

			ArgumentCaptor<ProductImage> captor = ArgumentCaptor.forClass(ProductImage.class);
			verify(imageRepository).save(captor.capture());

			// fileName = UUID + "_" + originalFilename
			assertThat(captor.getValue().getFileName()).contains("photo.png");
		}
	}

	// =========================================================================
	// updateProductImage
	// =========================================================================



	// =========================================================================
	// deleteProductImage
	// =========================================================================

	@Nested
	@DisplayName("deleteProductImage()")
	class DeleteProductImage {

		@Test
		@DisplayName("Existing image → file deleted from disk and record deleted from DB")
		void validDelete_deletesFileAndRecord() throws IOException {
			Product product = buildProduct(1L);
			ProductImage image = buildProductImage(1L, "delete_me.jpg", product);

			when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

			try (MockedStatic<Files> filesMock = mockStatic(Files.class);
			     MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {

				Path filePath = mock(Path.class);
				pathsMock.when(() -> Paths.get("uploads/products/" + "delete_me.jpg"))
						.thenReturn(filePath);
				filesMock.when(() -> Files.deleteIfExists(filePath)).thenReturn(true);

				productImageService.deleteProductImage(1L);
			}

			// DB record deleted — cast to ProductImage to avoid ambiguous method call
			verify(imageRepository).delete((ProductImage) image);
		}

		@Test
		@DisplayName("Image not found → RuntimeException")
		void imageNotFound_throwsException() {
			when(imageRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> productImageService.deleteProductImage(99L))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Image Not Found");

			verify(imageRepository, never()).delete((ProductImage) any());
		}

		@Test
		@DisplayName("IOException during file delete → RuntimeException with 'Image delete failed'")
		void ioException_throwsRuntimeException() throws IOException {
			Product product = buildProduct(1L);
			ProductImage image = buildProductImage(1L, "bad.jpg", product);

			when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

			try (MockedStatic<Files> filesMock = mockStatic(Files.class);
			     MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {

				Path filePath = mock(Path.class);
				pathsMock.when(() -> Paths.get("uploads/products/" + "bad.jpg"))
						.thenReturn(filePath);
				filesMock.when(() -> Files.deleteIfExists(filePath))
						.thenThrow(new IOException("Disk error"));

				assertThatThrownBy(() -> productImageService.deleteProductImage(1L))
						.isInstanceOf(RuntimeException.class)
						.hasMessageContaining("Image delete failed");
			}

			verify(imageRepository, never()).delete((ProductImage) any());
		}

		@Test
		@DisplayName("deleteProductImageFallback → throws RuntimeException with unavailable message")
		void deleteFallback_throwsRuntimeException() {
			Throwable t = new RuntimeException("CB triggered");

			assertThatThrownBy(() -> productImageService.deleteProductImageFallback(1L, t))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("Image delete service unavailable");
		}
	}
}