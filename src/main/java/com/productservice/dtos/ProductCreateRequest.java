package com.productservice.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProductCreateRequest {
	@NotBlank
	private String title;

	private String description;

	@NotBlank
	private String author;

	@NotBlank
	private String isbn;

	@NotNull
	@Min(1)
	@Max(1000)
	private Double price;

	@NotNull
	private Long categoryId;

}