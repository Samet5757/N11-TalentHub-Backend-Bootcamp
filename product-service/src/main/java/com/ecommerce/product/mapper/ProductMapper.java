package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public Product toEntity(ProductRequest request) {
        return new Product(
                request.name(),
                request.description(),
                request.brand(),
                request.price(),
                request.stock(),
                request.categoryId(),
                request.sellerId(),
                request.imageUrl(),
                request.badgeType()
        );
    }

    public void updateEntity(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategoryId(request.categoryId());
        product.setSellerId(request.sellerId());
        product.setImageUrl(request.imageUrl());
        product.setBadgeType(request.badgeType());
        product.setActive(request.active() == null || request.active());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getPrice(),
                product.getStock(),
                product.getCategoryId(),
                product.getSellerId(),
                product.getImageUrl(),
                product.getRatingAverage(),
                product.getReviewCount(),
                product.getActive(),
                product.getBadgeType(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
