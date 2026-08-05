package com.ecommerce.order_service.controllers;

import com.ecommerce.order_service.dto.APIResponse.APIResponse;
import com.ecommerce.order_service.dto.request.CreateOrderRequestDTO;
import com.ecommerce.order_service.dto.response.OrderDTO;
import com.ecommerce.order_service.exceptions.ConflictException;
import com.ecommerce.order_service.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping()
    public ResponseEntity<APIResponse<List<OrderDTO>>> getAll()
            throws ConflictException {
        return APIResponse.build(
                201,
                "Order fetched successful",
                orderService.getAllOrders()
        );
    }

    @PostMapping()
    public ResponseEntity<APIResponse<OrderDTO>> register(@Valid @RequestBody CreateOrderRequestDTO request)
            throws ConflictException {
        return APIResponse.build(
                201,
                "Order Created successful",
                orderService.createOrder(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<OrderDTO>> getUserById(@PathVariable String id){
        return APIResponse.build(
                200,
                "Order fetched Successfully",
                orderService.getOrderById(UUID.fromString(id))
        );
    }
}
