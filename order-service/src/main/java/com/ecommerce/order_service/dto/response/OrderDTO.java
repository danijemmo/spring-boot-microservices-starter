package com.ecommerce.order_service.dto.response;

import com.ecommerce.order_service.entities.Order;

import java.util.UUID;

public record OrderDTO(
        UUID id,
        String userId,
        String amount,
        String status
) {
    public static OrderDTO fromEntity(Order entity){
        return new OrderDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getStatus()
        );
    }
}
