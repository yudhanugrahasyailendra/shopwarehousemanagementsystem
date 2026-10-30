package com.example.inventory.service;

import com.example.inventory.dto.PageResponse;
import com.example.inventory.dto.SaleLineRequest;
import com.example.inventory.dto.SaleRequest;
import com.example.inventory.dto.SaleResponse;
import com.example.inventory.exception.OutOfStockException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.model.Sale;
import com.example.inventory.model.SaleLine;
import com.example.inventory.model.Variant;
import com.example.inventory.repository.SaleRepository;
import com.example.inventory.repository.VariantRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final VariantRepository variantRepository;

    public SaleService(SaleRepository saleRepository, VariantRepository variantRepository) {
        this.saleRepository = saleRepository;
        this.variantRepository = variantRepository;
    }

    /**
     * Sells the requested variants, all-or-nothing.
     * <ul>
     *   <li>Each line takes its stock with one atomic conditional UPDATE (see
     *       {@link VariantRepository#decrementStockIfAvailable}), so stock can never go negative,
     *       even when many customers buy the last units at the same moment.</li>
     *   <li>If any line cannot be fulfilled, an exception rolls back the whole transaction,
     *       so lines already processed get their stock back and no partial sale is recorded.</li>
     *   <li>Lines are processed in ascending variant id order so concurrent multi-line sales
     *       always lock rows in the same order and cannot deadlock each other.</li>
     * </ul>
     */
    @Transactional
    public SaleResponse checkout(SaleRequest request) {
        // Merge repeated variants into one quantity; TreeMap gives the deterministic lock order.
        Map<Long, Integer> quantityByVariant = new TreeMap<>();
        for (SaleLineRequest line : request.lines()) {
            quantityByVariant.merge(line.variantId(), line.quantity(), Integer::sum);
        }

        Sale sale = new Sale();
        for (Map.Entry<Long, Integer> entry : quantityByVariant.entrySet()) {
            Long variantId = entry.getKey();
            int quantity = entry.getValue();

            Variant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));

            // Snapshot what we need BEFORE the update; none of it depends on the stock level.
            SaleLine line = new SaleLine(variantId, variant.getSku(), variant.getItem().getName(),
                    quantity, variant.getEffectivePrice());

            if (variantRepository.decrementStockIfAvailable(variantId, quantity) == 0) {
                int available = variantRepository.findStockQuantityById(variantId);
                throw new OutOfStockException(variant.getSku(), quantity, available);
            }
            sale.addLine(line);
        }

        return SaleResponse.from(saleRepository.save(sale));
    }

    @Transactional(readOnly = true)
    public SaleResponse get(Long id) {
        return saleRepository.findById(id)
                .map(SaleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> list(Pageable pageable) {
        return PageResponse.from(saleRepository.findAll(pageable).map(SaleResponse::from));
    }
}
