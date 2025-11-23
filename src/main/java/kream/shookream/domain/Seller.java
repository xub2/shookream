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
@Table(name = "sellers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seller_id")
    private Long id;

    private String sellerName;

    private String sellerEmail;

    @Enumerated(EnumType.STRING)
    private SellerType sellerType;

    private String sellerPhoneNumber;

    private LocalDateTime sellerRegisterDate;

    @OneToMany(mappedBy = "seller") // event 의 seller
    private List<Event> sellingEvents = new ArrayList<>();

    @OneToMany(mappedBy = "seller") // ticket 의 seller
    private List<Ticket> tickets = new ArrayList<>();

    @Builder
    public Seller(String sellerName, String sellerEmail, SellerType sellerType, String sellerPhoneNumber, LocalDateTime sellerRegisterDate) {
        this.sellerName = sellerName;
        this.sellerEmail = sellerEmail;
        this.sellerType = sellerType;
        this.sellerPhoneNumber = sellerPhoneNumber;
        this.sellerRegisterDate = sellerRegisterDate;
    }
}
