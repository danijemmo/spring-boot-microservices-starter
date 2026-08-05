package com.ecommerce.order_service.repositories;

import com.ecommerce.order_service.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
