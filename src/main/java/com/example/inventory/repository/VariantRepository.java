package com.example.inventory.repository;

import com.example.inventory.model.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VariantRepository extends JpaRepository<Variant, Long> {

    List<Variant> findByItemIdOrderByIdAsc(Long itemId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /**
     * Atomically takes {@code quantity} units out of stock, but only if enough are available.
     * <p>
     * This is the core of the "never sell out-of-stock items" guarantee. The check and the
     * decrement happen in ONE SQL statement, so two concurrent buyers can never both take the
     * last unit (no "read, check, write" race window).
     *
     * @return 1 if the stock was reduced, 0 if there was not enough stock
     */
    @Modifying
    @Query("update Variant v set v.stockQuantity = v.stockQuantity - :quantity "
            + "where v.id = :id and v.stockQuantity >= :quantity")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * Atomically adds {@code delta} (positive = restock, negative = write-off) to the stock,
     * refusing any change that would make the stock negative.
     *
     * @return 1 if applied, 0 if it would have gone below zero
     */
    @Modifying(clearAutomatically = true)
    @Query("update Variant v set v.stockQuantity = v.stockQuantity + :delta "
            + "where v.id = :id and v.stockQuantity + :delta >= 0")
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);

    @Query("select v.stockQuantity from Variant v where v.id = :id")
    int findStockQuantityById(@Param("id") Long id);
}
