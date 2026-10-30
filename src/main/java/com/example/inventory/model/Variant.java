package com.example.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * A sellable unit of an {@link Item} (e.g. "Classic T-Shirt, size M, black").
 * This is the entity that holds stock and that is actually sold.
 */
@Entity
@Table(name = "variants",
        uniqueConstraints = @UniqueConstraint(name = "uk_variants_sku", columnNames = "sku"))
public class Variant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 50)
    private String size;

    @Column(length = 50)
    private String color;

    /** Optional override. When null, the item's base price applies. */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockQuantity;

    protected Variant() {
        // required by JPA
    }

    public Variant(String sku, String size, String color, BigDecimal price, int stockQuantity) {
        this.sku = sku;
        this.size = size;
        this.color = color;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    /** The price a customer pays: the variant's own price, or the item's base price as a fallback. */
    public BigDecimal getEffectivePrice() {
        return price != null ? price : item.getBasePrice();
    }

    public Long getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    /** Links this variant to its item (owning side of the relationship). */
    public void attachTo(Item item) {
        this.item = item;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    /**
     * Only for initial values. Day-to-day stock changes must go through the atomic
     * repository queries so concurrent requests cannot overwrite each other.
     */
    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
