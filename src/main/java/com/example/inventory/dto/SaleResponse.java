package com.example.inventory.dto;

import com.example.inventory.model.Sale;
import com.example.inventory.model.SaleLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        Instant createdAt,
        BigDecimal total,
        List<Line> lines) {

    public record Line(
            Long variantId,
            String sku,
            String itemName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {

        static Line from(SaleLine l) {
            return new Line(l.getVariantId(), l.getSku(), l.getItemName(),
                    l.getQuantity(), l.getUnitPrice(), l.getLineTotal());
        }
    }

    public static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getCreatedAt(),
                sale.getTotal(),
                sale.getLines().stream().map(Line::from).toList());
    }
}
