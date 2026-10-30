package com.example.inventory.controller;

import com.example.inventory.dto.PageResponse;
import com.example.inventory.dto.SaleRequest;
import com.example.inventory.dto.SaleResponse;
import com.example.inventory.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "Sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    @Operation(summary = "Sell variants. Fails with 409 OUT_OF_STOCK (and changes nothing) if any line lacks stock")
    public ResponseEntity<SaleResponse> checkout(@Valid @RequestBody SaleRequest request) {
        SaleResponse sale = saleService.checkout(request);
        return ResponseEntity.created(URI.create("/api/sales/" + sale.id())).body(sale);
    }

    @GetMapping
    @Operation(summary = "List past sales (newest first)")
    public PageResponse<SaleResponse> list(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return saleService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one sale")
    public SaleResponse get(@PathVariable Long id) {
        return saleService.get(id);
    }
}
