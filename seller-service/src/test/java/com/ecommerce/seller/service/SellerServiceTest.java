package com.ecommerce.seller.service;

import com.ecommerce.seller.dto.SellerRequest;
import com.ecommerce.seller.dto.SellerResponse;
import com.ecommerce.seller.entity.Seller;
import com.ecommerce.seller.exception.SellerNotFoundException;
import com.ecommerce.seller.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private SellerService sellerService;

    private Seller seller;

    @BeforeEach
    void setUp() {
        seller = new Seller();
        seller.setId(7L);
        seller.setStoreName("Store");
        seller.setTaxNumber("TX-1");
        seller.setRatingAverage(4.2);
        seller.setIsOfficialStore(true);
    }

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

    @Test
    void getSellerById_whenFound_shouldReturnResponse() {
        when(sellerRepository.findById(7L)).thenReturn(Optional.of(seller));

        SellerResponse response = sellerService.getSellerById(7L);

        assertEquals(7L, response.id());
        assertEquals("Store", response.storeName());
    }

    @Test
    void deleteSeller_shouldDeleteWhenExists() {
        when(sellerRepository.findById(7L)).thenReturn(Optional.of(seller));

        sellerService.deleteSeller(7L);

        verify(sellerRepository).delete(seller);
    }

    @Test
    void getSellers_shouldMapPageContent() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(sellerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(seller), pageable, 1));

        Page<SellerResponse> page = sellerService.getSellers(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("Store", page.getContent().get(0).storeName());
    }

    @Test
    void createSeller_whenInvalidRequest_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> sellerService.createSeller(null));
        assertThrows(IllegalArgumentException.class, () -> sellerService.createSeller(new SellerRequest("", "tx", 3.0, true)));
        assertThrows(IllegalArgumentException.class, () -> sellerService.createSeller(new SellerRequest("S", "", 3.0, true)));
        assertThrows(IllegalArgumentException.class, () -> sellerService.createSeller(new SellerRequest("S", "tx", 6.0, true)));
        assertThrows(IllegalArgumentException.class, () -> sellerService.createSeller(new SellerRequest("S", "tx", 3.0, null)));
    }
}
