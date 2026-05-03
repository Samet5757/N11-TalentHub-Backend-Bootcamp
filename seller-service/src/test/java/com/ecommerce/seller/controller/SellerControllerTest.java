package com.ecommerce.seller.controller;

import com.ecommerce.seller.dto.SellerRequest;
import com.ecommerce.seller.dto.SellerResponse;
import com.ecommerce.seller.entity.Seller;
import com.ecommerce.seller.repository.SellerRepository;
import com.ecommerce.seller.service.SellerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerControllerTest {

    @Mock
    private SellerRepository sellerRepository;

    private SellerController sellerController;

    @BeforeEach
    void setUp() {
        SellerService sellerService = new SellerService(sellerRepository);
        sellerController = new SellerController(sellerService);
    }

    @Test
    void getSellers_shouldReturnPagedResponse() {
        PageRequest pageable = PageRequest.of(0, 10);
        Seller seller = new Seller();
        seller.setId(1L);
        seller.setStoreName("Store");
        seller.setTaxNumber("1234567890");
        seller.setRatingAverage(4.5);
        seller.setIsOfficialStore(true);
        when(sellerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(seller), pageable, 1));

        Page<SellerResponse> page = sellerController.getSellers(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("Store", page.getContent().get(0).storeName());
    }

    @Test
    void createSeller_shouldReturnCreated() {
        SellerRequest request = new SellerRequest("Store", "1234567890", 4.5, true);
        Seller saved = new Seller();
        saved.setId(3L);
        saved.setStoreName("Store");
        saved.setTaxNumber("1234567890");
        saved.setRatingAverage(4.5);
        saved.setIsOfficialStore(true);
        when(sellerRepository.save(any(Seller.class))).thenReturn(saved);

        ResponseEntity<SellerResponse> response = sellerController.createSeller(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(3L, response.getBody().id());
    }

    @Test
    void deleteSeller_shouldReturnNoContent() {
        Seller existing = new Seller();
        existing.setId(11L);
        existing.setStoreName("Store");
        existing.setTaxNumber("1234567890");
        existing.setRatingAverage(4.5);
        existing.setIsOfficialStore(true);
        when(sellerRepository.findById(11L)).thenReturn(java.util.Optional.of(existing));

        ResponseEntity<Void> response = sellerController.deleteSeller(11L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(sellerRepository).delete(existing);
    }
}
