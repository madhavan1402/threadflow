package com.threadflow.buyer.portal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BuyerPortalDtos {
    private BuyerPortalDtos() {}

    public record BuyerOverviewResponse(
            Long buyerId,
            String buyerName,
            String country,
            int totalOrders,
            int inProgressOrders,
            int completedOrders,
            int confirmedWarningsCount
    ) {}

    public record BuyerMilestoneProgress(
            Long id,
            String milestoneName,
            Integer sequenceNumber,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate actualCompletionDate,
            String status
    ) {}

    public record BuyerConfirmedWarningResponse(
            Long id,
            String riskStatus,
            String message,
            Instant confirmedAt
    ) {}

    public record BuyerApprovedPhotoResponse(
            Long id,
            String originalFilename,
            String contentType,
            Long fileSize,
            Instant approvedAt
    ) {}

    public record BuyerOrderSummaryResponse(
            Long id,
            String poNumber,
            String styleName,
            String productDescription,
            Integer totalQuantity,
            LocalDate exFactoryDate,
            String status,
            int progressPercent,
            int confirmedWarningsCount,
            int approvedPhotosCount
    ) {}

    public record BuyerOrderDetailResponse(
            Long id,
            String poNumber,
            String styleName,
            String productDescription,
            Integer totalQuantity,
            LocalDate exFactoryDate,
            String status,
            int progressPercent,
            List<BuyerMilestoneProgress> milestones,
            List<BuyerConfirmedWarningResponse> confirmedWarnings,
            List<BuyerApprovedPhotoResponse> approvedPhotos
    ) {}
}
