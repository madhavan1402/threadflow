package com.threadflow.buyer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class BuyerDtos {
    private BuyerDtos() {}
    public record BuyerRequest(@NotBlank String buyerName, @NotBlank String country,
                               @Email String contactEmail) {}
    public record BuyerResponse(Long id, String buyerName, String country, String contactEmail) {}
}
