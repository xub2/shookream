package kream.shookream.domain;

import jakarta.persistence.*;
import kream.shookream.domain.embedded.EventTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events", indexes = {
        // IDX_EVENT_SELLER_TIME: 판매자별 이벤트 목록을 시작일시(start_time) 최신순으로 조회
        @Index(name = "IDX_EVENT_SELLER_TIME", columnList = "seller_id, start_time DESC"),

        // IDX_EVENT_SEARCH: 이벤트 분류(event_type)를 필터링하고 시작일시(start_time)로 정렬하여 검색
        @Index(name = "IDX_EVENT_SEARCH", columnList = "eventType, start_time DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @OneToMany(mappedBy = "event")
    private List<Ticket> tickets = new ArrayList<>();

    //    @Column(nullable = false)
    private Integer maxTicketCount;

    //    @Column(nullable = false)
    private Integer currentTicketStockCount;

    private String eventName;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Embedded
    private EventTime eventTime;

    @Builder
    public Event(Venue venue, Seller seller, String eventName, EventType eventType, EventTime eventTime, Integer maxTicketCount) {
        this.venue = venue;
        this.seller = seller;
        this.eventName = eventName;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.maxTicketCount = maxTicketCount;
        this.currentTicketStockCount = maxTicketCount; // 생성 시점에는 동일
    }

    public void decreaseTicketStock() {
        if (this.currentTicketStockCount <= 0) {
            throw new IllegalStateException("표 재고가 부족합니다.");
        }

        this.currentTicketStockCount--;
    }

    // update 쿼리 나감
    public void increaseTicketStock() {
        if (this.currentTicketStockCount >= this.maxTicketCount) {
            throw new RuntimeException("판매 가능 수량을 초과하였습니다.");
        }

        this.currentTicketStockCount++;
    }
}
