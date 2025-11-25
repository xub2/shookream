package kream.shookream.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets", indexes = {
        // IDX_TICKET_EVENT_STATUS: 특정 이벤트의 'AVAILABLE' 티켓을 빠르게 검색
        @Index(name = "IDX_TICKET_EVENT_STATUS", columnList = "event_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    // 하나의 이벤트에는 여러 티켓 가능 1:N
    // 즉, Event안에 Ticket 객체가 maxTicketCount 만큼 존재함.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    // 한명의 판매자는 여러 티켓 판매한다.
    // 하나의 티켓은 한명의 판매자에게 판매되어진다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @OneToMany(mappedBy = "ticket")
    private List<OrderTicket> orderTickets = new ArrayList<>();

    private String seatInfo;


    // 원가
    private Integer ticketPrice;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Builder
    public Ticket(Event event, Seller seller, String seatInfo, Integer ticketPrice, TicketStatus status) {
        this.event = event;
        this.seller = seller;
        this.seatInfo = seatInfo;
        this.ticketPrice = ticketPrice;
        this.status = status;
    }

    // 티켓 판매 가능한지 검사
    public boolean isAvailable() {
        return this.status == TicketStatus.AVAILABLE;
    }

    public void sell() {
        if (!isAvailable()) {
            throw new IllegalStateException("이미 매진된 표입니다.");
        }

        this.status = TicketStatus.SOLDOUT;
    }

    public void revertToAvailable() {
        if (this.status != TicketStatus.SOLDOUT) {
            throw new IllegalStateException("취소가 불가능한 표입니다.");
        }

        this.status = TicketStatus.AVAILABLE;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
