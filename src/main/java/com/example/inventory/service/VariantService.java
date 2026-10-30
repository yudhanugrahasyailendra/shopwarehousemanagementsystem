package com.example.inventory.service;

import com.example.inventory.dto.StockAdjustmentRequest;
import com.example.inventory.dto.VariantCreateRequest;
import com.example.inventory.dto.VariantResponse;
import com.example.inventory.dto.VariantUpdateRequest;
import com.example.inventory.exception.DuplicateSkuException;
import com.example.inventory.exception.InvalidRequestException;
import com.example.inventory.exception.OutOfStockException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.model.Item;
import com.example.inventory.model.Variant;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.VariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VariantService {

    private final ItemRepository itemRepository;
    private final VariantRepository variantRepository;

    public VariantService(ItemRepository itemRepository, VariantRepository variantRepository) {
        this.itemRepository = itemRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional
    public VariantResponse create(Long itemId, VariantCreateRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", itemId));
        if (variantRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        Variant variant = new Variant(request.sku(), request.size(), request.color(),
                request.price(), request.stockQuantity());
        variant.attachTo(item);
        return VariantResponse.from(variantRepository.save(variant));
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listByItem(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item", itemId);
        }
        return variantRepository.findByItemIdOrderByIdAsc(itemId).stream()
                .map(VariantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VariantResponse get(Long id) {
        return VariantResponse.from(findVariant(id));
    }

    @Transactional
    public VariantResponse update(Long id, VariantUpdateRequest request) {
        Variant variant = findVariant(id);
        if (variantRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new DuplicateSkuException(request.sku());
        }
        variant.setSku(request.sku());
        variant.setSize(request.size());
        variant.setColor(request.color());
        variant.setPrice(request.price());
        return VariantResponse.from(variant);
    }

    @Transactional
    public void delete(Long id) {
        variantRepository.delete(findVariant(id));
    }

    /**
     * Restock (positive delta) or write off (negative delta) units.
     * The change is a single atomic SQL update, so it cannot be lost when it races with a sale,
     * and the stock can never be pushed below zero.
     */
    @Transactional
    public VariantResponse adjustStock(Long id, StockAdjustmentRequest request) {
        int delta = request.delta();
        if (delta == 0) {
            throw new InvalidRequestException("'delta' must not be 0");
        }
        if (!variantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Variant", id);
        }
        if (variantRepository.adjustStock(id, delta) == 0) {
            int available = variantRepository.findStockQuantityById(id);
            Variant variant = findVariant(id);
            throw new OutOfStockException(variant.getSku(), -delta, available);
        }
        return VariantResponse.from(findVariant(id));
    }

    private Variant findVariant(Long id) {
        return variantRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Variant", id));
    }
}
