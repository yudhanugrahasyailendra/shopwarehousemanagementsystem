package com.example.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleLineRequest(
        @NotNull Long variantId,
        @NotNull @Min(1) @Max(100_000) Integer quantity) {
}
