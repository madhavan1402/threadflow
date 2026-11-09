package com.threadflow.buyer.portal;

import com.threadflow.production.ProductionPhoto;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buyer")
@PreAuthorize("hasRole('BUYER')")
public class BuyerPortalController {
    private final BuyerPortalService portalService;

    public BuyerPortalController(BuyerPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/portal")
    public BuyerPortalDtos.BuyerOverviewResponse portal(Authentication authentication) {
        return portalService.overview(authentication);
    }

    @GetMapping("/orders")
    public List<BuyerPortalDtos.BuyerOrderSummaryResponse> orders(Authentication authentication) {
        return portalService.orders(authentication);
    }

    @GetMapping("/orders/{orderId}")
    public BuyerPortalDtos.BuyerOrderDetailResponse orderDetail(
            @PathVariable Long orderId, Authentication authentication) {
        return portalService.orderDetail(orderId, authentication);
    }

    @GetMapping("/orders/{orderId}/milestones")
    public List<BuyerPortalDtos.BuyerMilestoneProgress> milestones(
            @PathVariable Long orderId, Authentication authentication) {
        return portalService.milestones(orderId, authentication);
    }

    @GetMapping("/orders/{orderId}/photos")
    public List<BuyerPortalDtos.BuyerApprovedPhotoResponse> photos(
            @PathVariable Long orderId, Authentication authentication) {
        return portalService.approvedPhotos(orderId, authentication);
    }

    @GetMapping("/orders/{orderId}/photos/{photoId}/content")
    public ResponseEntity<Resource> photoContent(
            @PathVariable Long orderId,
            @PathVariable Long photoId,
            Authentication authentication) {
        ProductionPhoto photo = portalService.getApprovedPhoto(orderId, photoId, authentication);
        Resource resource = portalService.loadPhotoResource(orderId, photoId, authentication);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(photo.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getOriginalFilename() + "\"")
                .body(resource);
    }
}
