package kream.shookream.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "venue_id")
    private Long id;

    private String venueName;

    private Integer capacity;

    private String venuePhoneNumber;

    @OneToMany(mappedBy = "venue")
    private List<Event> eventHeld = new ArrayList<>();

    @Builder
    public Venue(String venueName, Integer capacity, String venuePhoneNumber) {
        this.venueName = venueName;
        this.capacity = capacity;
        this.venuePhoneNumber = venuePhoneNumber;
    }
}
