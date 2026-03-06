package com.productservice.specifications;
import org.springframework.data.jpa.domain.Specification;
import com.productservice.models.Product;
public class ProductSpecification
{
	public static Specification<Product> hasTitle(String title) {
		return (root, query, cb) ->
				title == null ? null :
						cb.like(cb.lower(root.get("title")),
								"%" + title.toLowerCase() + "%");
	}

	public static Specification<Product> hasAuthor(String author) {
		return (root, query, cb) ->
				author == null ? null :
						cb.like(cb.lower(root.get("author")),
								"%" + author.toLowerCase() + "%");
	}

	public static Specification<Product> hasIsbn(String isbn) {
		return (root, query, cb) ->
				isbn == null ? null :
						cb.equal(root.get("isbn"), isbn);
	}

	public static Specification<Product> hasCategory(Long categoryId) {
		return (root, query, cb) ->
				categoryId == null ? null :
						cb.equal(root.get("category").get("id"), categoryId);
	}

	public static Specification<Product> priceGreaterThanOrEqual(Double minPrice) {
		return (root, query, cb) ->
				minPrice == null ? null :
						cb.greaterThanOrEqualTo(root.get("price"), minPrice);
	}

	public static Specification<Product> priceLessThanOrEqual(Double maxPrice) {
		return (root, query, cb) ->
				maxPrice == null ? null :
						cb.lessThanOrEqualTo(root.get("price"), maxPrice);
	}
}
