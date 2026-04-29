package com.ecommerce.seller.service;

import com.ecommerce.seller.dto.SellerRequest;
import com.ecommerce.seller.dto.SellerResponse;
import com.ecommerce.seller.entity.Seller;
import com.ecommerce.seller.exception.SellerNotFoundException;
import com.ecommerce.seller.repository.SellerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;

    public SellerService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    public Page<SellerResponse> getSellers(Pageable pageable) {
        return sellerRepository.findAll(pageable).map(this::toResponse);
    }

    public SellerResponse getSellerById(Long id) {
        return toResponse(findSeller(id));
    }

    public SellerResponse createSeller(SellerRequest request) {
        validateRequest(request);
        Seller seller = new Seller();
        seller.setStoreName(request.storeName());
        seller.setTaxNumber(request.taxNumber());
        seller.setRatingAverage(request.ratingAverage());
        seller.setIsOfficialStore(request.isOfficialStore());
        return toResponse(sellerRepository.save(seller));
    }

    public void deleteSeller(Long id) {
        sellerRepository.delete(findSeller(id));
    }

    private Seller findSeller(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new SellerNotFoundException(id));
    }

    private SellerResponse toResponse(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getStoreName(),
                seller.getTaxNumber(),
                seller.getRatingAverage(),
                seller.getIsOfficialStore()
        );
    }

    private void validateRequest(SellerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Seller request cannot be empty");
        }
        if (request.storeName() == null || request.storeName().isBlank()) {
            throw new IllegalArgumentException("Store name is required");
        }
        if (request.taxNumber() == null || request.taxNumber().isBlank()) {
            throw new IllegalArgumentException("Tax number is required");
        }
        if (request.ratingAverage() == null || request.ratingAverage() < 0 || request.ratingAverage() > 5) {
            throw new IllegalArgumentException("Rating average must be between 0 and 5");
        }
        if (request.isOfficialStore() == null) {
            throw new IllegalArgumentException("Official store flag is required");
        }
    }
}
