package com.example.inventory.dto;

import com.example.inventory.model.Variant;

import java.math.BigDecimal;

public record VariantResponse(
        Long id,
        Long itemId,
        String sku,
        String size,
        String color,
        /* The variant's own price override; null when the item's base price applies. */
        BigDecimal price,
        /* The price a customer actually pays. */
        BigDecimal effectivePrice,
        int stockQuantity,
        boolean inStock) {

    public static VariantResponse from(Variant v) {
        return new VariantResponse(
                v.getId(),
                v.getItem().getId(),
                v.getSku(),
                v.getSize(),
                v.getColor(),
                v.getPrice(),
                v.getEffectivePrice(),
                v.getStockQuantity(),
                v.getStockQuantity() > 0);
    }
}
