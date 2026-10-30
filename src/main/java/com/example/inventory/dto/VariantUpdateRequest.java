package com.example.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Stock is intentionally absent: it can only change through the stock-adjustment endpoint or a sale. */
public record VariantUpdateRequest(
        @NotBlank @Size(max = 64) String sku,
        @Size(max = 50) String size,
        @Size(max = 50) String color,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price) {
}
