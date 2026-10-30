package com.example.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaleRequest(
        @NotEmpty @Size(max = 100) List<@Valid SaleLineRequest> lines) {
}
