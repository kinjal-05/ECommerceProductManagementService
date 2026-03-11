package com.productservice.services;

import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService
{
	void uploadProductImage(Long ProductId, MultipartFile image);
	void updateProductImage(Long ProductId,MultipartFile newImage);
	void deleteProductImage(Long ImageId);
}