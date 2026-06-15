package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.entity.Item;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {

}