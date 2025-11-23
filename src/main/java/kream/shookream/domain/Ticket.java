package kream.shookream.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    // 하나의 이벤트에는 여러 티켓 가능 1:N
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

    private Integer ticketPrice;

    @Enumerated
    private TicketStatus status;

    @Builder
    public Ticket(Event event, Seller seller, String seatInfo, Integer ticketPrice, TicketStatus status) {
        this.event = event;
        this.seller = seller;
        this.seatInfo = seatInfo;
        this.ticketPrice = ticketPrice;
        this.status = status;
    }
}
