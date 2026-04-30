package com.ecommerce.seller.service;

import com.ecommerce.seller.dto.SellerRequest;
import com.ecommerce.seller.dto.SellerResponse;
import com.ecommerce.seller.entity.Seller;
import com.ecommerce.seller.exception.SellerNotFoundException;
import com.ecommerce.seller.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private SellerService sellerService;

    @Test
    void createSeller_shouldPersistAndReturnResponse() {
        SellerRequest request = new SellerRequest("Nova Magaza", "1234567890", 4.6, true);

        Seller saved = new Seller();
        saved.setId(11L);
        saved.setStoreName(request.storeName());
        saved.setTaxNumber(request.taxNumber());
        saved.setRatingAverage(request.ratingAverage());
        saved.setIsOfficialStore(request.isOfficialStore());

        when(sellerRepository.save(any(Seller.class))).thenReturn(saved);

        SellerResponse response = sellerService.createSeller(request);

        assertEquals(11L, response.id());
        assertEquals("Nova Magaza", response.storeName());
        assertEquals("1234567890", response.taxNumber());
        assertEquals(4.6, response.ratingAverage());
        assertTrue(response.isOfficialStore());
    }

    @Test
    void getSellerById_whenNotFound_shouldThrow() {
        when(sellerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SellerNotFoundException.class, () -> sellerService.getSellerById(99L));
    }
}
