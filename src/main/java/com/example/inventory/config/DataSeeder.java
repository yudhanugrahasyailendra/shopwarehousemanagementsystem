package com.example.inventory.config;

import com.example.inventory.dto.ItemCreateRequest;
import com.example.inventory.dto.VariantCreateRequest;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Loads a small demo catalogue at startup. Disable with {@code app.seed.enabled=false}. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ItemService itemService;
    private final ItemRepository itemRepository;

    public DataSeeder(ItemService itemService, ItemRepository itemRepository) {
        this.itemService = itemService;
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(String... args) {
        if (itemRepository.count() > 0) {
            return;
        }

        // Variants with their own prices and different stock levels
        itemService.create(new ItemCreateRequest(
                "Classic T-Shirt", "100% cotton crew-neck t-shirt", new BigDecimal("19.99"),
                List.of(
                        new VariantCreateRequest("TSHIRT-BLK-S", "S", "Black", null, 25),
                        new VariantCreateRequest("TSHIRT-BLK-M", "M", "Black", null, 40),
                        new VariantCreateRequest("TSHIRT-BLK-L", "L", "Black", null, 10),
                        new VariantCreateRequest("TSHIRT-WHT-M", "M", "White", new BigDecimal("21.99"), 3)),
                null));

        // One variant is already sold out: handy for trying the out-of-stock rule
        itemService.create(new ItemCreateRequest(
                "Running Sneakers", "Lightweight daily trainers", new BigDecimal("89.00"),
                List.of(
                        new VariantCreateRequest("SNKR-RED-42", "42", "Red", null, 8),
                        new VariantCreateRequest("SNKR-RED-43", "43", "Red", null, 0),
                        new VariantCreateRequest("SNKR-BLU-42", "42", "Blue", new BigDecimal("94.50"), 5)),
                null));

        // No variants: gets one automatic default variant carrying the stock
        itemService.create(new ItemCreateRequest(
                "Ceramic Mug", "350 ml mug, dishwasher safe", new BigDecimal("7.50"),
                null, 60));

        log.info("Seeded {} sample items", itemRepository.count());
    }
}
