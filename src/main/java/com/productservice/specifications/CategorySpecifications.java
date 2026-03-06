package com.productservice.specifications;
import com.productservice.models.Category;
import org.springframework.data.jpa.domain.Specification;
public class CategorySpecifications
{
	private CategorySpecifications() {}

	public static Specification<Category> build(
			String name,
			Integer displayOrder) {

		return Specification
				.where(hasName(name))
				.and(hasDisplayOrder(displayOrder));
	}

	private static Specification<Category> hasName(String name) {
		if (name == null || name.isBlank()) return null;

		return (root, query, cb) ->
				cb.like(cb.lower(root.get("name")),
						"%" + name.toLowerCase() + "%");
	}

	private static Specification<Category> hasDisplayOrder(Integer displayOrder) {
		if (displayOrder == null) return null;

		return (root, query, cb) ->
				cb.equal(root.get("displayOrder"), displayOrder);
	}
}
