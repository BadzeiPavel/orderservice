package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.entity.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID>,
    JpaSpecificationExecutor<Order> {

  @EntityGraph(attributePaths = "orderItems")
  Optional<Order> findByIdAndDeletedFalse(UUID id);

  @EntityGraph(attributePaths = "orderItems")
  Page<Order> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

  @EntityGraph(attributePaths = "orderItems")
  Page<Order> findAllOrders(Specification<Order> spec, Pageable pageable);

  @Modifying
  @Query("UPDATE Order o SET o.deleted = true WHERE o.id = :id")
  int softDeleteById(@Param("id") UUID id);
}