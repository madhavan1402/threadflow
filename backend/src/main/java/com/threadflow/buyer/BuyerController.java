package com.threadflow.buyer;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/buyers")
public class BuyerController {
    private final BuyerService service;
    public BuyerController(BuyerService service) { this.service = service; }
    @GetMapping public List<BuyerDtos.BuyerResponse> list() { return service.list(); }
    @PostMapping
    @PreAuthorize("hasRole('MERCHANDISER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BuyerDtos.BuyerResponse create(@Valid @RequestBody BuyerDtos.BuyerRequest request) {
        return service.create(request);
    }
}
