package com.example.inventory.service;

import com.example.inventory.dto.ItemCreateRequest;
import com.example.inventory.dto.ItemResponse;
import com.example.inventory.dto.ItemUpdateRequest;
import com.example.inventory.dto.PageResponse;
import com.example.inventory.dto.VariantCreateRequest;
import com.example.inventory.exception.DuplicateSkuException;
import com.example.inventory.exception.InvalidRequestException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.model.Item;
import com.example.inventory.model.Variant;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.VariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;

    public ItemService(ItemRepository itemRepository, VariantRepository variantRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        List<VariantCreateRequest> variants = request.variants() == null ? List.of() : request.variants();

        if (!variants.isEmpty() && request.initialStock() != null) {
            throw new InvalidRequestException(
                    "'initialStock' only applies to items without variants; set stockQuantity on each variant instead");
        }
        validateSkus(variants);

        Item item = itemRepository.save(new Item(request.name(), request.description(), request.basePrice()));

        if (variants.isEmpty()) {
            // Items without variants still need somewhere to keep their stock: one automatic default variant.
            int stock = request.initialStock() == null ? 0 : request.initialStock();
            item.addVariant(new Variant("ITEM-" + item.getId() + "-DEFAULT", null, null, null, stock));
        } else {
            variants.forEach(v -> item.addVariant(
                    new Variant(v.sku(), v.size(), v.color(), v.price(), v.stockQuantity())));
        }

        itemRepository.flush(); // persists the variants now so the response contains their generated ids
        return ItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> list(String nameFilter, Pageable pageable) {
        Page<Item> page = (nameFilter == null || nameFilter.isBlank())
                ? itemRepository.findAll(pageable)
                : itemRepository.findByNameContainingIgnoreCase(nameFilter.trim(), pageable);
        return PageResponse.from(page.map(ItemResponse::from));
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        return ItemResponse.from(findItem(id));
    }

    @Transactional
    public ItemResponse update(Long id, ItemUpdateRequest request) {
        Item item = findItem(id);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setBasePrice(request.basePrice());
        return ItemResponse.from(item);
    }

    /** Deleting an item also deletes its variants. Past sales are unaffected (they store a snapshot). */
    @Transactional
    public void delete(Long id) {
        itemRepository.delete(findItem(id));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Item", id));
    }

    private void validateSkus(List<VariantCreateRequest> variants) {
        Set<String> seen = new HashSet<>();
        for (VariantCreateRequest v : variants) {
            if (!seen.add(v.sku())) {
                throw new DuplicateSkuException(v.sku());
            }
            if (variantRepository.existsBySku(v.sku())) {
                throw new DuplicateSkuException(v.sku());
            }
        }
    }
}
