package kream.shookream.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    private Long memberId;
    private List<Long> ticketIds;
}
