package kream.shookream.controller.dto;

import lombok.Data;

@Data
public class OrderResponse {

    private Long orderID;
    private String status;
    private Integer totalAmount;

    public OrderResponse(Long orderID, String status, Integer totalAmount) {
        this.orderID = orderID;
        this.status = status;
        this.totalAmount = totalAmount;
    }
}
