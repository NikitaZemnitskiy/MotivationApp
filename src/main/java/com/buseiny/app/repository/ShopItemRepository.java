package com.buseiny.app.repository;

import com.buseiny.app.model.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {}
