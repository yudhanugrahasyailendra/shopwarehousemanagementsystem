package com.example.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VariantCreateRequest(
        @NotBlank @Size(max = 64) String sku,
        @Size(max = 50) String size,
        @Size(max = 50) String color,
        /* Optional. When omitted the item's base price is used. */
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull @Min(0) @Max(10_000_000) Integer stockQuantity) {
}
