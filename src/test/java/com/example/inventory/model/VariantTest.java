package com.example.inventory.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VariantTest {

    @Test
    void usesItsOwnPriceWhenSet() {
        Item item = new Item("Shirt", null, new BigDecimal("10.00"));
        Variant variant = new Variant("SH-M", "M", null, new BigDecimal("12.50"), 5);
        item.addVariant(variant);

        assertThat(variant.getEffectivePrice()).isEqualByComparingTo("12.50");
    }

    @Test
    void fallsBackToTheItemBasePriceWhenNoOwnPrice() {
        Item item = new Item("Shirt", null, new BigDecimal("10.00"));
        Variant variant = new Variant("SH-S", "S", null, null, 5);
        item.addVariant(variant);

        assertThat(variant.getEffectivePrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void itemTotalStockIsTheSumOfItsVariants() {
        Item item = new Item("Shirt", null, new BigDecimal("10.00"));
        item.addVariant(new Variant("SH-S", "S", null, null, 5));
        item.addVariant(new Variant("SH-M", "M", null, null, 7));

        assertThat(item.getTotalStock()).isEqualTo(12);
    }
}
