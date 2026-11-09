package com.threadflow.production;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/production")
public class ProductionController {
    private final ProductionService service;
    public ProductionController(ProductionService service) { this.service = service; }

    @GetMapping("/available-orders")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<ProductionDtos.AvailableOrderResponse> availableOrders(Authentication authentication) {
        return service.availableOrders(authentication);
    }

    @GetMapping("/orders/{orderId}/milestones")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<ProductionDtos.MilestoneResponse> milestones(@PathVariable Long orderId, Authentication authentication) {
        return service.milestones(orderId, authentication);
    }

    @PostMapping(value="/updates", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ProductionDtos.UpdateResponse submit(
            @RequestParam Long orderId,
            @RequestParam Long milestoneId,
            @RequestParam LocalDate productionDate,
            @RequestParam Integer completedQuantity,
            @RequestParam(required=false) String issueNote,
            @RequestParam String idempotencyKey,
            @RequestParam(value="photos", required=false) List<MultipartFile> photos,
            Authentication authentication) {
        if (issueNote != null && issueNote.length() > 5000) throw new IllegalArgumentException("Issue note exceeds 5000 characters");
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 64) throw new IllegalArgumentException("Invalid idempotency key");
        return service.submit(new ProductionDtos.CreateUpdateRequest(orderId, milestoneId, productionDate,
                completedQuantity, issueNote, idempotencyKey), photos, authentication);
    }

    @GetMapping("/my-updates")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<ProductionDtos.UpdateResponse> myUpdates(Authentication authentication) { return service.myUpdates(authentication); }

    @GetMapping("/updates")
    @PreAuthorize("hasRole('MERCHANDISER')")
    public List<ProductionDtos.UpdateResponse> updates(Authentication authentication) { return service.allUpdates(authentication); }

    @GetMapping("/orders/{orderId}/updates")
    public List<ProductionDtos.UpdateResponse> orderUpdates(@PathVariable Long orderId, Authentication authentication) {
        return service.orderUpdates(orderId, authentication);
    }

    @GetMapping("/milestones/{milestoneId}/updates")
    public List<ProductionDtos.UpdateResponse> milestoneUpdates(@PathVariable Long milestoneId, Authentication authentication) {
        return service.milestoneUpdates(milestoneId, authentication);
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable Long photoId, Authentication authentication) {
        ProductionPhoto photo = service.getAuthorizedPhoto(photoId, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .body(service.photoBytes(photo));
    }
}
