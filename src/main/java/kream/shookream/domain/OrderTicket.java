package kream.shookream.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_ticket", indexes = {
        // IDX_OT_TICKET_TIME: 특정 티켓의 판매 기간별 정산 데이터를 조회
        @Index(name = "IDX_OT_TICKET_TIME", columnList = "ticket_id, createdAt ASC")
})
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

    // 구매 시점의 최종가
    private Integer purchase_price;

    @Builder
    public OrderTicket(Order order, Ticket ticket, LocalDateTime createdAt, Integer purchase_price) {
        this.order = order;
        this.ticket = ticket;
        this.createdAt = createdAt;
        this.purchase_price = purchase_price;
    }

    public void cancelPurchase() {
        // 1. 연관된 Ticket 객체의 상태를 AVAILABLE로 복구
        Ticket ticket = this.getTicket(); // @Getter로 접근
        ticket.revertToAvailable();       // Ticket.java에 구현된 메서드

        // 2. Ticket이 복구되었으므로, 해당 Ticket이 속한 Event의 재고를 증가시켜야 합니다.
        // Event 객체는 Ticket 내부에 참조되고 있습니다.
        Event event = ticket.getEvent(); // Ticket.java에 Event 객체 Getter가 있다고 가정
        event.increaseTicketStock();           // Event.java에 구현된 재고 증가 메서드
    }

    // 필요한 필드만 넣기
    public void setOrder(Order order) {
        this.order = order;
    }


}
