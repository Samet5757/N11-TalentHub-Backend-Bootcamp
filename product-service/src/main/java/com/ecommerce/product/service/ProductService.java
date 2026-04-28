package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public Page<ProductResponse> listProducts(String search, Long categoryId, Long sellerId, Boolean active,
                                              Pageable pageable) {
        return productRepository
                .findAll(ProductSpecifications.matches(search, categoryId, sellerId, active), pageable)
                .map(productMapper::toResponse);
    }

    public ProductResponse getProduct(Long productId) {
        return productMapper.toResponse(findProduct(productId));
    }

    public ProductResponse updateStock(Long productId, Integer newStock) {
        validateStock(newStock);
        Product product = findProduct(productId);
        product.setStock(newStock);
        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse createProduct(ProductRequest request) {
        validateProductRequest(request);
        Product product = productMapper.toEntity(request);
        product.setActive(request.active() == null || request.active());
        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        validateProductRequest(request);
        Product product = findProduct(productId);
        productMapper.updateEntity(product, request);
        return productMapper.toResponse(productRepository.save(product));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void validateProductRequest(ProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Product request cannot be empty");
        }
        requireText(request.name(), "Product name is required");
        requireText(request.brand(), "Product brand is required");
        requirePositive(request.price(), "Product price must be greater than zero");
        validateStock(request.stock());
        requirePositive(request.categoryId(), "Category id must be greater than zero");
        requirePositive(request.sellerId(), "Seller id must be greater than zero");
    }

    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
