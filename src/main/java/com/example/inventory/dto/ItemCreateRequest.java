package com.example.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Either send {@code variants}, or send nothing and (optionally) an {@code initialStock}:
 * an item without variants gets one automatic "default" variant that carries its stock.
 */
public record ItemCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal basePrice,
        @Valid @Size(max = 100) List<VariantCreateRequest> variants,
        @Min(0) @Max(10_000_000) Integer initialStock) {
}
