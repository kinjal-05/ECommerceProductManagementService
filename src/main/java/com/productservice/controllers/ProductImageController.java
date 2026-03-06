package com.productservice.controllers;

import com.productservice.services.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/product-images/v1")
@Tag(name = "Product Images", description = "APIs for managing product images")
@RequiredArgsConstructor
public class ProductImageController {
	private final ProductImageService imageService;

	@Operation(summary = "Upload product image", description = "Upload an image file for a product")
	@ApiResponse(responseCode = "200", description = "Image uploaded successfully")
	@PostMapping(value = "/product/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> uploadImage(@RequestHeader("X-USER-ID") @PathVariable Long productId,
	                                          @Parameter(description = "Image file to upload", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary"))) @RequestParam("image") MultipartFile image) {
		imageService.uploadProductImage(productId, image);
		return ResponseEntity.ok("Image upload successfully");
	}

	@Operation(summary = "Update product image", description = "Update an existing product image")
	@PutMapping(value = "updateimage/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> updateImage(@RequestHeader("X-USER-ID") @PathVariable Long imageId,
	                                          @RequestParam("image") MultipartFile image) {
		imageService.updateProductImage(imageId, image);
		return ResponseEntity.ok("Image updated successfully");
	}

	@Operation(summary = "Delete product image", description = "Delete product image by image ID")
	@DeleteMapping("deleteimage/{imageId}")
	public ResponseEntity<String> deleteImage(@RequestHeader("X-USER-ID") @PathVariable Long imageId) {
		imageService.deleteProductImage(imageId);
		return ResponseEntity.ok("Image Deleted Successfully");
	}

}
