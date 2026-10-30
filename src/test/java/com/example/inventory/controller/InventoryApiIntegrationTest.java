package com.example.inventory.controller;

import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.SaleRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests of the HTTP API (real Spring context, in-memory H2). */
@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

    private static final String SHIRT = """
            {"name":"Shirt","description":"Cotton","basePrice":10.00,
             "variants":[
               {"sku":"SH-S","size":"S","color":"Black","stockQuantity":3},
               {"sku":"SH-M","size":"M","color":"Black","price":12.50,"stockQuantity":0}]}
            """;

    @Autowired
    private MockMvc mvc;
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
    void createsAnItemWithVariantsAndComputesPricesAndStock() throws Exception {
        mvc.perform(json(post("/api/items"), SHIRT))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Shirt"))
                .andExpect(jsonPath("$.totalStock").value(3))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.variants.length()").value(2))
                .andExpect(jsonPath("$.variants[0].effectivePrice").value(10.00))   // falls back to base price
                .andExpect(jsonPath("$.variants[1].effectivePrice").value(12.50))   // own price
                .andExpect(jsonPath("$.variants[1].inStock").value(false));
    }

    @Test
    void anItemWithoutVariantsGetsADefaultVariantCarryingTheStock() throws Exception {
        mvc.perform(json(post("/api/items"), """
                        {"name":"Mug","basePrice":7.50,"initialStock":60}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants.length()").value(1))
                .andExpect(jsonPath("$.variants[0].stockQuantity").value(60))
                .andExpect(jsonPath("$.totalStock").value(60));
    }

    @Test
    void rejectsInvalidInputWithFieldLevelErrors() throws Exception {
        mvc.perform(json(post("/api/items"), """
                        {"name":"","basePrice":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.basePrice").exists());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(json(post("/api/items"), "{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsADuplicateSku() throws Exception {
        mvc.perform(json(post("/api/items"), SHIRT)).andExpect(status().isCreated());

        mvc.perform(json(post("/api/items"), SHIRT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_SKU"));
    }

    @Test
    void unknownResourcesAndRoutesAreReportedAs404() throws Exception {
        mvc.perform(get("/api/items/987654"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void outOfStockVariantsCannotBeSold() throws Exception {
        String item = createShirt();
        long inStock = variantId(item, 0);
        long soldOut = variantId(item, 1);

        // 3 in stock: selling 3 works and is priced at the base price
        mvc.perform(json(post("/api/sales"), sale(inStock, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(30.00));

        // ...the 4th unit does not exist anymore
        mvc.perform(json(post("/api/sales"), sale(inStock, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("OUT_OF_STOCK"));

        // ...and a variant created with 0 stock can never be sold
        mvc.perform(json(post("/api/sales"), sale(soldOut, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("OUT_OF_STOCK"));

        mvc.perform(get("/api/variants/" + inStock))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    void restockingMakesAVariantSellableAgainAndStockCannotGoNegative() throws Exception {
        long soldOut = variantId(createShirt(), 1);

        mvc.perform(json(patch("/api/variants/" + soldOut + "/stock"), "{\"delta\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(5));

        mvc.perform(json(post("/api/sales"), sale(soldOut, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(25.00));

        mvc.perform(json(patch("/api/variants/" + soldOut + "/stock"), "{\"delta\":-10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("OUT_OF_STOCK"));

        mvc.perform(json(patch("/api/variants/" + soldOut + "/stock"), "{\"delta\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void updatesAndDeletesAnItem() throws Exception {
        long id = itemId(createShirt());

        mvc.perform(json(put("/api/items/" + id), """
                        {"name":"Premium Shirt","description":"Better","basePrice":15.00}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium Shirt"))
                .andExpect(jsonPath("$.variants[0].effectivePrice").value(15.00));

        mvc.perform(get("/api/items").param("q", "premium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(delete("/api/items/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/items/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void managesVariantsOfAnExistingItem() throws Exception {
        long itemId = itemId(createShirt());

        String created = mvc.perform(json(post("/api/items/" + itemId + "/variants"), """
                        {"sku":"SH-L","size":"L","color":"Black","stockQuantity":9}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long variantId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(get("/api/items/" + itemId + "/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mvc.perform(json(put("/api/variants/" + variantId), """
                        {"sku":"SH-L","size":"L","color":"Black","price":11.00}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectivePrice").value(11.00))
                .andExpect(jsonPath("$.stockQuantity").value(9));   // PUT never touches stock

        mvc.perform(delete("/api/variants/" + variantId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/variants/" + variantId)).andExpect(status().isNotFound());
    }

    // ---- helpers -------------------------------------------------------------------------

    private String createShirt() throws Exception {
        return mvc.perform(json(post("/api/items"), SHIRT))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static long itemId(String itemJson) {
        return ((Number) JsonPath.read(itemJson, "$.id")).longValue();
    }

    private static long variantId(String itemJson, int index) {
        return ((Number) JsonPath.read(itemJson, "$.variants[" + index + "].id")).longValue();
    }

    private static String sale(long variantId, int quantity) {
        return "{\"lines\":[{\"variantId\":" + variantId + ",\"quantity\":" + quantity + "}]}";
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
