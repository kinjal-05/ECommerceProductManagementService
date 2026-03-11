package com.productservice.servicesImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import com.productservice.models.Product;
import com.productservice.models.ProductImage;
import com.productservice.repositories.ProductImageRepository;
import com.productservice.repositories.ProductRepository;
import com.productservice.services.ProductImageService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

	private final ProductRepository productRepository;
	private final ProductImageRepository imageRepository;
	private static final String UPLOAD_DIR = "uploads/products/";

	@Override
	public void uploadProductImage(Long productId, MultipartFile image) {
		try {
			Files.createDirectories(Paths.get(UPLOAD_DIR));
			String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
			Path filePath = Paths.get(UPLOAD_DIR + fileName);
			Files.write(filePath, image.getBytes());
			Product product = productRepository.findById(productId)
					.orElseThrow(() -> new RuntimeException("Product not found"));
			ProductImage productImage = ProductImage.builder().fileName(fileName).product(product).build();
			imageRepository.save(productImage);
		} catch (IOException e) {
			throw new RuntimeException("Image upload failed", e);
		}
	}

	@Override
	@CircuitBreaker(name = "productImageCB", fallbackMethod = "updateProductImageFallback")
	public void updateProductImage(Long imageId, MultipartFile newImage) {
		ProductImage existingImage = imageRepository.findById(imageId)
				.orElseThrow(() -> new RuntimeException("Image not found"));
		try {
			Path oldPath = Paths.get(UPLOAD_DIR + existingImage.getFileName());
			Files.deleteIfExists(oldPath);
			String newFileName = UUID.randomUUID() + "_" + newImage.getOriginalFilename();
			Path newPath = Paths.get(UPLOAD_DIR, newFileName);
			Files.write(newPath, newImage.getBytes());
			existingImage.setFileName(newFileName);
			imageRepository.save(existingImage);
		} catch (IOException e) {
			throw new RuntimeException("Image update failed", e);
		}
	}

	public void updateProductImageFallback(Long imageId, MultipartFile newImage, Throwable t) {
		throw new RuntimeException("Image update service unavailable. Please try again later.", t);
	}

	@Override
	@CircuitBreaker(name = "productImageCB", fallbackMethod = "deleteProductImageFallback")
	public void deleteProductImage(Long imageId) {
		ProductImage image = imageRepository.findById(imageId)
				.orElseThrow(() -> new RuntimeException("Image Not Found"));
		try {
			Path path = Paths.get(UPLOAD_DIR + image.getFileName());
			Files.deleteIfExists(path);
			imageRepository.delete(image);
		} catch (IOException e) {
			throw new RuntimeException("Image delete failed", e);
		}
	}

	public void deleteProductImageFallback(Long imageId, Throwable t) {
		throw new RuntimeException("Image delete service unavailable. Please try again later.", t);
	}
}