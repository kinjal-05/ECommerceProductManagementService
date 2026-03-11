package com.productservice.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CategoryCreateRequest {
	@NotBlank(message = "Category name is required")
	@Size(max = 30)
	private String name;

	@NotNull(message = "Display order is required")
	@Min(value = 1, message = "Display order must be at least 1")
	@Max(value = 100, message = "Display order must not exceed 100")
	private Integer displayOrder;

}
