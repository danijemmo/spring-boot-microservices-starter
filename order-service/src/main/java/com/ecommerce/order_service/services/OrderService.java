package com.ecommerce.order_service.services;

import com.ecommerce.grpc.user.UserResponse;
import com.ecommerce.order_service.dto.request.CreateOrderRequestDTO;
import com.ecommerce.order_service.dto.response.OrderDTO;
import com.ecommerce.order_service.entities.Order;
import com.ecommerce.order_service.exceptions.NotFoundException;
import com.ecommerce.order_service.grpc.UserClient;
import com.ecommerce.order_service.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserClient userClient;

    public Order _getOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("user not found"));
    }

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream().map(OrderDTO::fromEntity).toList();
    }

    public OrderDTO createOrder(CreateOrderRequestDTO reqDTO) {
        UserResponse user = userClient.getUser(reqDTO.userId());

        if (user == null) {
            throw new NotFoundException("user not found");
        }

        Order order = Order.builder()
                .amount(reqDTO.amount())
                .userId(reqDTO.userId())
                .status("CREATED")
                .build();

        Order savedOrder = orderRepository.save(order);

        return OrderDTO.fromEntity(savedOrder);
    }

    public OrderDTO getOrderById(UUID id){
        return OrderDTO.fromEntity(_getOrder(id));
    }
}
