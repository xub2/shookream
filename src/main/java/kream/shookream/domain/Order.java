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

    @OneToMany(mappedBy = "order")
    private List<OrderTicket> orderTickets = new ArrayList<>();

    private LocalDateTime orderedAt;

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
}
