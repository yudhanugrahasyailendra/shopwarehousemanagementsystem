package com.example.inventory.controller;

import com.example.inventory.dto.StockAdjustmentRequest;
import com.example.inventory.dto.VariantCreateRequest;
import com.example.inventory.dto.VariantResponse;
import com.example.inventory.dto.VariantUpdateRequest;
import com.example.inventory.service.VariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Variants & stock")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping("/api/items/{itemId}/variants")
    @Operation(summary = "Add a variant to an item")
    public ResponseEntity<VariantResponse> create(@PathVariable Long itemId,
                                                  @Valid @RequestBody VariantCreateRequest request) {
        VariantResponse created = variantService.create(itemId, request);
        return ResponseEntity.created(URI.create("/api/variants/" + created.id())).body(created);
    }

    @GetMapping("/api/items/{itemId}/variants")
    @Operation(summary = "List the variants of an item")
    public List<VariantResponse> listByItem(@PathVariable Long itemId) {
        return variantService.listByItem(itemId);
    }

    @GetMapping("/api/variants/{id}")
    @Operation(summary = "Get one variant")
    public VariantResponse get(@PathVariable Long id) {
        return variantService.get(id);
    }

    @PutMapping("/api/variants/{id}")
    @Operation(summary = "Update a variant's sku, size, color and price (not its stock)")
    public VariantResponse update(@PathVariable Long id, @Valid @RequestBody VariantUpdateRequest request) {
        return variantService.update(id, request);
    }

    @DeleteMapping("/api/variants/{id}")
    @Operation(summary = "Delete a variant")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        variantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/variants/{id}/stock")
    @Operation(summary = "Adjust stock by a relative amount: positive = restock, negative = write-off")
    public VariantResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return variantService.adjustStock(id, request);
    }
}
