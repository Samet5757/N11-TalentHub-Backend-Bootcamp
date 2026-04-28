package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, new ProductMapper());
    }

    @Test
    void createProductSavesMarketplaceCatalogFields() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            return product;
        });

        ProductResponse response = productService.createProduct(validRequest());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("xDrive Toryum Oyuncu Koltugu");
        assertThat(response.brand()).isEqualTo("xDrive");
        assertThat(response.categoryId()).isEqualTo(15L);
        assertThat(response.sellerId()).isEqualTo(42L);
        assertThat(response.active()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProductRejectsInvalidPrice() {
        ProductRequest request = new ProductRequest(
                "Oyuncu Koltugu",
                "Ergonomik koltuk",
                "xDrive",
                BigDecimal.ZERO,
                10,
                15L,
                42L,
                "https://cdn.example.com/chair.jpg",
                "N11_CHOICE",
                true
        );

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product price must be greater than zero");
    }

    @Test
    void getProductThrowsDomainExceptionWhenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found with id: 99");
    }

    @Test
    void updateStockRejectsNegativeStock() {
        assertThatThrownBy(() -> productService.updateStock(1L, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product stock cannot be negative");
    }

    private ProductRequest validRequest() {
        return new ProductRequest(
                "xDrive Toryum Oyuncu Koltugu",
                "Kumas, siyah oyuncu koltugu",
                "xDrive",
                new BigDecimal("7499.90"),
                10,
                15L,
                42L,
                "https://cdn.example.com/chair.jpg",
                "N11_CHOICE",
                true
        );
    }
}
