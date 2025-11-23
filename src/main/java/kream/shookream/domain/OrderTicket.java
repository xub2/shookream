package kream.shookream.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    private LocalDateTime createdAt;

    private Integer purchase_price;

    @Builder
    public OrderTicket(Order order, Ticket ticket, LocalDateTime createdAt, Integer purchase_price) {
        this.order = order;
        this.ticket = ticket;
        this.createdAt = createdAt;
        this.purchase_price = purchase_price;
    }
}
