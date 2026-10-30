package com.example.inventory.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A product the shop sells (e.g. "Classic T-Shirt").
 * Stock is NOT stored here: it lives on {@link Variant}, and an item's total stock is the sum of its variants.
 */
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Default price, used by every variant that does not define its own price. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variant> variants = new ArrayList<>();

    protected Item() {
        // required by JPA
    }

    public Item(String name, String description, BigDecimal basePrice) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public void addVariant(Variant variant) {
        variant.attachTo(this);
        variants.add(variant);
    }

    public int getTotalStock() {
        return variants.stream().mapToInt(Variant::getStockQuantity).sum();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Variant> getVariants() {
        return variants;
    }
}
