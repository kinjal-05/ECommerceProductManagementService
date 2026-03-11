package com.productservice.controllers;

import com.productservice.services.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * All route paths resolved from api-paths.yml at startup.
 *
 * api.product-image.base   → /api/product-images/v1
 * api.product-image.upload → /product/{productId}
 * api.product-image.update → /update/{imageId}
 * api.product-image.delete → /delete/{imageId}
 *
 * Bugs fixed from original:
 * - @RequestHeader + @PathVariable on same param → separated into email + id
 * - updateimage/{imageId} and deleteimage/{imageId} had no separator slash → fixed in yml
 * - Response messages standardized to consistent casing
 */
@RestController
@RequestMapping("${api.product-image.base}")
@Tag(name = "Product Images", description = "APIs for managing product images")
@RequiredArgsConstructor
public class ProductImageController {

	private final ProductImageService imageService;

	@Operation(summary = "Upload product image", description = "Upload an image file for a product")
	@ApiResponse(responseCode = "200", description = "Image uploaded successfully")
	@PostMapping(
			value = "${api.product-image.upload}",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ResponseEntity<String> uploadImage(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long productId,
			@Parameter(
					description = "Image file to upload",
					content = @Content(
							mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
							schema = @Schema(type = "string", format = "binary")
					)
			)
			@RequestParam("image") MultipartFile image) {

		imageService.uploadProductImage(productId, image);
		return ResponseEntity.ok("Image uploaded successfully");
	}

	@Operation(summary = "Update product image", description = "Update an existing product image")
	@PutMapping(
			value = "${api.product-image.update}",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ResponseEntity<String> updateImage(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long imageId,
			@RequestParam("image") MultipartFile image) {

		imageService.updateProductImage(imageId, image);
		return ResponseEntity.ok("Image updated successfully");
	}

	@Operation(summary = "Delete product image", description = "Delete product image by image ID")
	@DeleteMapping("${api.product-image.delete}")
	public ResponseEntity<String> deleteImage(
			@RequestHeader("X-USER-ID") String email,
			@PathVariable Long imageId) {

		imageService.deleteProductImage(imageId);
		return ResponseEntity.ok("Image deleted successfully");
	}
}