package com.example.inventory.repository;

import com.example.inventory.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
