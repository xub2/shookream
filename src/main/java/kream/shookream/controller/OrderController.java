package kream.shookream.controller;

import kream.shookream.controller.dto.OrderRequest;
import kream.shookream.controller.dto.OrderResponse;
import kream.shookream.domain.Order;
import kream.shookream.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders
     * 티켓 주문 생성 + 재고 감소
     * 성공시 201 Created 반환
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {

        Order createdOrder = orderService.createOrder(request.getMemberId(), request.getTicketIds());

        OrderResponse response = new OrderResponse(
                createdOrder.getId(),
                createdOrder.getStatus().name(),
                createdOrder.getTotalOrderAmount()
        );

        return ResponseEntity.created(URI.create("/api/orders" + createdOrder.getId())).body(response);
    }

    /**
     * DELETE /api/orders/{orderId}
     * 주문 취소 + 티켓 상태 및 재고 복구
     * 성공시 204 No Content 반환
     */
    @DeleteMapping("{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable("orderId") Long orderId) {
        orderService.cancelOrder(orderId);

        return ResponseEntity.noContent().build();
    }
}
