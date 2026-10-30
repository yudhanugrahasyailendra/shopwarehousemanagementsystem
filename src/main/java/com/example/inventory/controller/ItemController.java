package com.example.inventory.controller;

import com.example.inventory.dto.ItemCreateRequest;
import com.example.inventory.dto.ItemResponse;
import com.example.inventory.dto.ItemUpdateRequest;
import com.example.inventory.dto.PageResponse;
import com.example.inventory.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @Operation(summary = "Create an item, optionally with its variants")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemCreateRequest request) {
        ItemResponse created = itemService.create(request);
        return ResponseEntity.created(URI.create("/api/items/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List items (paginated). Use ?q= to search by name, ?page=, ?size=, ?sort=name,asc")
    public PageResponse<ItemResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return itemService.list(q, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one item with its variants and stock")
    public ItemResponse get(@PathVariable Long id) {
        return itemService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an item's name, description and base price")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemUpdateRequest request) {
        return itemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an item and all of its variants")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
