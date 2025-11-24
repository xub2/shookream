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
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "order" , cascade = CascadeType.ALL)
    private List<OrderTicket> orderTickets = new ArrayList<>();

    private LocalDateTime orderedAt;

    // 모든 purchase_price의 가격의 합
    private Integer totalOrderAmount;

    @Enumerated
    private OrderStatus status;

    @Builder
    public Order(Member member, LocalDateTime orderedAt, Integer totalOrderAmount, OrderStatus status) {
        this.member = member;
        this.orderedAt = orderedAt; // 객체 초기화시 LocalDateTime.now()
        this.totalOrderAmount = totalOrderAmount;
        this.status = status;
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.SUCCESS;
    }

    // 주문을 취소 상태로 만들고 연관된 모든 OrderTicket에 취소 로직 위임
    public void cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("현재 주문은 취소할 수 없습니다."); // 아마 이미 CANCELED 라고 되어 있을 듯?
        }

        // 1. 주문 상태 변경
        this.status = OrderStatus.CANCELED;

        // 2. OrderTicket에 취소 책임 위임 -> Order의 원자성 -> 개별 티켓 취소 불가 -> 전체 Order 에 담긴 티켓을 모두 취소해야함
        for (OrderTicket orderTicket : this.orderTickets) {
            orderTicket.cancelPurchase();
        }
    }

    // 양방향 연관관계 메서드 Order <-> OrderTicket
    public void addOrderTicket(OrderTicket orderTicket) {
        this.orderTickets.add(orderTicket);
        orderTicket.setOrder(this);
    }

    // 주문 생성 -> 회원과 티켓 필요
    public static Order createOrder(Member member, List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            throw new IllegalStateException("티켓 목록은 비어있을 수 없음");
        }

        // 실제 주문 생성
        int totalAmount = tickets.stream()
                .mapToInt(Ticket::getTicketPrice)
                .sum();

        Order order = Order.builder()
                .member(member)
                .orderedAt(LocalDateTime.now())
                .totalOrderAmount(totalAmount)
                .status(OrderStatus.SUCCESS)
                .build();

        for (Ticket ticket : tickets) {
            ticket.sell(); // 여기서 해당 티켓들은 모두 SOLDOUT 으로 변경
            ticket.getEvent().decreaseTicketStock();

            OrderTicket orderTicket = OrderTicket.builder()
                    .ticket(ticket)
                    .createdAt(LocalDateTime.now())
                    .purchase_price(ticket.getTicketPrice())
                    .build();

            order.addOrderTicket(orderTicket);
        }

        return order;
    }


}
