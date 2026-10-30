package com.example.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** A relative change: positive = restock, negative = write-off (damaged, lost...). Zero is rejected as pointless. */
public record StockAdjustmentRequest(
        @NotNull @Min(-1_000_000) @Max(1_000_000) Integer delta) {
}
