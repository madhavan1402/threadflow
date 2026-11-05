package com.threadflow.buyer;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuyerService {
    private final BuyerRepository repository;
    public BuyerService(BuyerRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<BuyerDtos.BuyerResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasRole('MERCHANDISER')")
    @Transactional
    public BuyerDtos.BuyerResponse create(BuyerDtos.BuyerRequest request) {
        if (repository.findByBuyerNameIgnoreCaseAndCountryIgnoreCase(request.buyerName().trim(), request.country().trim()).isPresent()) {
            throw new IllegalArgumentException("A buyer with this name and country already exists");
        }
        Buyer buyer = new Buyer(request.buyerName().trim(), request.country().trim(),
                request.contactEmail() == null || request.contactEmail().isBlank() ? null : request.contactEmail().trim());
        return toResponse(repository.save(buyer));
    }
    private BuyerDtos.BuyerResponse toResponse(Buyer b) {
        return new BuyerDtos.BuyerResponse(b.getId(), b.getBuyerName(), b.getCountry(), b.getContactEmail());
    }
}
