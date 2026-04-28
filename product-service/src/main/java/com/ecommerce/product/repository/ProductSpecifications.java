package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {
    private ProductSpecifications() {
    }

    public static Specification<Product> matches(String search, Long categoryId, Long sellerId, Boolean active) {
        return Specification
                .where(activeEquals(active))
                .and(categoryEquals(categoryId))
                .and(sellerEquals(sellerId))
                .and(searchContains(search));
    }

    private static Specification<Product> activeEquals(Boolean active) {
        return (root, query, criteriaBuilder) ->
                active == null ? null : criteriaBuilder.equal(root.get("active"), active);
    }

    private static Specification<Product> categoryEquals(Long categoryId) {
        return (root, query, criteriaBuilder) ->
                categoryId == null ? null : criteriaBuilder.equal(root.get("categoryId"), categoryId);
    }

    private static Specification<Product> sellerEquals(Long sellerId) {
        return (root, query, criteriaBuilder) ->
                sellerId == null ? null : criteriaBuilder.equal(root.get("sellerId"), sellerId);
    }

    private static Specification<Product> searchContains(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.toLowerCase().trim() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
            );
        };
    }
}
