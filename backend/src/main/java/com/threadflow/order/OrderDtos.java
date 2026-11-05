package com.threadflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}
    public record VariantRequest(@NotBlank String color, @NotBlank String size, @NotNull @Positive Integer quantity) {}
    public record CreateOrderRequest(
            @NotNull Long buyerId,
            @NotBlank @Size(max = 100) String poNumber,
            @NotBlank String styleName,
            String productDescription,
            @NotNull @Positive Integer totalQuantity,
            @NotNull LocalDate exFactoryDate,
            @NotNull Long templateId,
            @NotEmpty List<@Valid VariantRequest> variants
    ) {}
    public record UpdateOrderRequest(
            @NotNull Long buyerId,
            @NotBlank String styleName,
            String productDescription,
            @NotNull @Positive Integer totalQuantity,
            @NotNull LocalDate exFactoryDate,
            @NotBlank String status,
            @NotEmpty List<@Valid VariantRequest> variants
    ) {}
    public record VariantResponse(Long id, String color, String size, Integer quantity) {}
    public record OrderResponse(Long id, String poNumber, BuyerSummary buyer, String styleName,
                                String productDescription, Integer totalQuantity, LocalDate exFactoryDate,
                                String status, List<VariantResponse> variants, List<MilestoneResponse> milestones) {}
    public record BuyerSummary(Long id, String buyerName, String country) {}
    public record MilestoneResponse(Long id, String milestoneName, Integer sequenceNumber, LocalDate plannedStartDate,
                                    LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualCompletionDate,
                                    String status, String notes) {}
    public record MilestoneUpdateRequest(@NotBlank String status, LocalDate actualStartDate,
                                         LocalDate actualCompletionDate, String notes) {}
}
