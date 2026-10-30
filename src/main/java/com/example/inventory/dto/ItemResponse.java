package com.example.inventory.dto;

import com.example.inventory.model.Item;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        /* Sum of the stock of all variants. */
        int totalStock,
        boolean inStock,
        Instant createdAt,
        List<VariantResponse> variants) {

    public static ItemResponse from(Item item) {
        int totalStock = item.getTotalStock();
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getBasePrice(),
                totalStock,
                totalStock > 0,
                item.getCreatedAt(),
                item.getVariants().stream().map(VariantResponse::from).toList());
    }
}
