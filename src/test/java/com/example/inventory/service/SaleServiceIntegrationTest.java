package com.example.inventory.service;

import com.example.inventory.dto.ItemCreateRequest;
import com.example.inventory.dto.ItemResponse;
import com.example.inventory.dto.SaleLineRequest;
import com.example.inventory.dto.SaleRequest;
import com.example.inventory.dto.SaleResponse;
import com.example.inventory.dto.VariantCreateRequest;
import com.example.inventory.exception.OutOfStockException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.seed.enabled=false")
class SaleServiceIntegrationTest {

    @Autowired
    private ItemService itemService;
    @Autowired
    private VariantService variantService;
    @Autowired
    private SaleService saleService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private SaleRepository saleRepository;

    @BeforeEach
    void cleanDatabase() {
        saleRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    void sellingReducesStockAndUsesVariantPriceOrFallsBackToBasePrice() {
        ItemResponse item = itemService.create(new ItemCreateRequest(
                "Shirt", null, new BigDecimal("10.00"),
                List.of(new VariantCreateRequest("SH-S", "S", null, null, 5),
                        new VariantCreateRequest("SH-M", "M", null, new BigDecimal("12.50"), 5)),
                null));
        Long small = item.variants().get(0).id();
        Long medium = item.variants().get(1).id();

        SaleResponse sale = saleService.checkout(sale(line(small, 2), line(medium, 1)));

        assertThat(sale.total()).isEqualByComparingTo("32.50"); // 2 x 10.00 (base) + 1 x 12.50 (own price)
        assertThat(variantService.get(small).stockQuantity()).isEqualTo(3);
        assertThat(variantService.get(medium).stockQuantity()).isEqualTo(4);
    }

    @Test
    void rejectsASaleThatExceedsStockAndLeavesEverythingUntouched() {
        Long variant = singleVariantItem("MUG-1", 2);

        assertThatThrownBy(() -> saleService.checkout(sale(line(variant, 3))))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("requested 3, available 2");

        assertThat(variantService.get(variant).stockQuantity()).isEqualTo(2);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void theLastUnitCanBeSoldButNotTheNextOne() {
        Long variant = singleVariantItem("MUG-2", 1);

        saleService.checkout(sale(line(variant, 1)));

        assertThat(variantService.get(variant).stockQuantity()).isZero();
        assertThat(variantService.get(variant).inStock()).isFalse();
        assertThatThrownBy(() -> saleService.checkout(sale(line(variant, 1))))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    void multiLineSaleIsAllOrNothing() {
        ItemResponse item = itemService.create(new ItemCreateRequest(
                "Shoes", null, new BigDecimal("50.00"),
                List.of(new VariantCreateRequest("SHOE-A", null, null, null, 5),
                        new VariantCreateRequest("SHOE-B", null, null, null, 1)),
                null));
        Long plentiful = item.variants().get(0).id();
        Long scarce = item.variants().get(1).id();

        // Lines are processed in id order: the first line succeeds, the second one fails.
        assertThatThrownBy(() -> saleService.checkout(sale(line(plentiful, 2), line(scarce, 2))))
                .isInstanceOf(OutOfStockException.class);

        assertThat(variantService.get(plentiful).stockQuantity()).as("first line must be rolled back").isEqualTo(5);
        assertThat(variantService.get(scarce).stockQuantity()).isEqualTo(1);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void repeatedLinesForTheSameVariantAreMerged() {
        Long variant = singleVariantItem("MUG-3", 5);

        SaleResponse sale = saleService.checkout(sale(line(variant, 2), line(variant, 2)));

        assertThat(sale.lines()).hasSize(1);
        assertThat(sale.lines().get(0).quantity()).isEqualTo(4);
        assertThat(variantService.get(variant).stockQuantity()).isEqualTo(1);
    }

    @Test
    void unknownVariantIsReportedAsNotFound() {
        assertThatThrownBy(() -> saleService.checkout(sale(line(999_999L, 1))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void concurrentBuyersNeverOversellTheLastUnits() throws Exception {
        int stock = 5;
        int buyers = 20;
        Long variant = singleVariantItem("HOT-ITEM", stock);

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch allReady = new CountDownLatch(buyers);
        CountDownLatch startGun = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < buyers; i++) {
            pool.submit(() -> {
                allReady.countDown();
                try {
                    startGun.await();
                    saleService.checkout(sale(line(variant, 1)));
                    succeeded.incrementAndGet();
                } catch (OutOfStockException e) {
                    rejected.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            });
        }
        allReady.await();
        startGun.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(unexpected).isEmpty();
        assertThat(succeeded.get()).as("only the available units can be sold").isEqualTo(stock);
        assertThat(rejected.get()).isEqualTo(buyers - stock);
        assertThat(variantService.get(variant).stockQuantity()).isZero();
        assertThat(saleRepository.count()).isEqualTo(stock);
    }

    private Long singleVariantItem(String sku, int stock) {
        return itemService.create(new ItemCreateRequest(
                        "Item " + sku, null, new BigDecimal("5.00"),
                        List.of(new VariantCreateRequest(sku, null, null, null, stock)), null))
                .variants().get(0).id();
    }

    private static SaleLineRequest line(Long variantId, int quantity) {
        return new SaleLineRequest(variantId, quantity);
    }

    private static SaleRequest sale(SaleLineRequest... lines) {
        return new SaleRequest(List.of(lines));
    }
}
