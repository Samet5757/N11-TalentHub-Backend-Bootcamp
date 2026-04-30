package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.OutOfStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Cacheable(value = "products")
    public Page<ProductResponse> listProducts(String search, Long categoryId, Long sellerId, Boolean active,
                                              int page, int size, String sortBy, String sortDir) {
        validatePageRequest(page, size);
        String sortField = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return productRepository
                .findAll(ProductSpecifications.matches(search, categoryId, sellerId, active), pageable)
                .map(productMapper::toResponse);
    }

    public ProductResponse getProduct(Long productId) {
        return productMapper.toResponse(findProduct(productId));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateStock(Long productId, Integer newStock) {
        validateStock(newStock);
        Product product = findProduct(productId);
        product.setStock(newStock);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void reserveStock(Long productId, Integer quantity) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId must be greater than zero");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        int updatedRows = productRepository.decrementStockAtomically(productId, quantity);
        if (updatedRows == 0) {
            throw new OutOfStockException(productId, quantity);
        }
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        validateProductRequest(request);
        Product product = productMapper.toEntity(request);
        product.setActive(request.active() == null || request.active());
        return productMapper.toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        validateProductRequest(request);
        Product product = findProduct(productId);
        productMapper.updateEntity(product, request);
        return productMapper.toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long productId) {
        Product product = findProduct(productId);
        productRepository.delete(product);
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

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
    }
}
