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
@Table(name = "events")
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

    private String eventName;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Embedded
    private EventTime eventTime;

    @Builder
    public Event(Venue venue, Seller seller, String eventName, EventType eventType, EventTime eventTime) {
        this.venue = venue;
        this.seller = seller;
        this.eventName = eventName;
        this.eventType = eventType;
        this.eventTime = eventTime;
    }

}
