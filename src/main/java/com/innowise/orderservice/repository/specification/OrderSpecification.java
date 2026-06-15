package com.innowise.orderservice.repository.specification;

import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.enums.Status;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecification {

  private OrderSpecification() { }


  public static Specification<Order> withFilters(UUID userId, LocalDateTime startDate,
      LocalDateTime endDate, List<Status> statuses) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.isFalse(root.get("deleted")));
      if (userId != null) {
        predicates.add(cb.equal(root.get("userId"), userId));
      }
      if (startDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
      }
      if (endDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
      }
      if (statuses != null && !statuses.isEmpty()) {
        predicates.add(root.get("status").in(statuses));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}