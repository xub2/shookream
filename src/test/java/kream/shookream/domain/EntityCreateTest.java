package kream.shookream.domain;

import kream.shookream.config.IntegrationTest;
import kream.shookream.domain.embedded.EventTime;
import kream.shookream.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@IntegrationTest
public class EntityCreateTest {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private SellerRepository sellerRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private OrderTicketRepository orderTicketRepository;


    @Test
    public void createAllEntityTest() {

        // 1. 의존성이 없는 엔티티를 먼저 생성해야함
        // Venue, Seller, Member

        // 1-1 Venue
        Venue venue = Venue.builder()
                .venueName("고양 킨텍스")
                .capacity(16000)
                .venuePhoneNumber("031-123-4567")
                .build();

        Venue savedVenue = venueRepository.save(venue);
        assertThat(savedVenue.getId()).isNotNull();

        // 1-2 Seller
        Seller seller = Seller.builder()
                .sellerName("YG엔터테인먼트")
                .sellerEmail("yg@exmaple.com")
                .sellerType(SellerType.COMPANY)
                .sellerPhoneNumber("02-1234-1234")
                .sellerRegisterDate(LocalDateTime.now())
                .build();

        Seller savedSeller = sellerRepository.save(seller);
        assertThat(savedSeller.getId()).isNotNull();

        // 1-3. Member
        Member member = Member.builder()
                .email("testMember@shookream.com")
                .phoneNumber("010-1111-2222")
                .password("encoded_password")
                .build();
        Member savedMember = memberRepository.save(member);
        assertThat(savedMember.getId()).isNotNull();

        // ------------------------------------------------------------

        // 2. Event 생성 -> Venue , Seller 의존성
        EventTime eventTime = new EventTime(
                LocalDateTime.of(2026, 7, 10, 19, 0),
                LocalDateTime.of(2026, 7, 10, 22, 0)
        );

        Event event = Event.builder()
                .venue(savedVenue)
                .seller(savedSeller)
                .eventName("지드래곤 월드투어 - 고양")
                .eventType(EventType.CONCERT)
                .eventTime(eventTime)
                .build();

        Event savedEvent = eventRepository.save(event);
        assertThat(savedEvent.getId()).isNotNull();

        // ------------------------------------------------------------

        // 3. Ticket -> Event , Seller 의존
        Ticket ticket = Ticket.builder()
                .event(savedEvent) // 참조
                .seller(savedSeller) // 참조
                .seatInfo("A구역 1층 5열 12번")
                .ticketPrice(150000)
                .status(TicketStatus.AVAILABLE) // Enum은 정의되었다고 가정
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        assertThat(savedTicket.getId()).isNotNull();

        // ------------------------------------------------------------

        // 4. Order -> Member 의존
        Order order = Order.builder()
                .member(savedMember) // 참조
                .orderedAt(LocalDateTime.now())
                .totalOrderAmount(150000)
                .status(OrderStatus.SUCCESS) // Enum은 정의되었다고 가정
                .build();

        Order savedOrder = orderRepository.save(order);
        assertThat(savedOrder.getId()).isNotNull();


        // 5. OrderTicket (주문 상세) 생성 및 저장: Order와 Ticket에 의존

        OrderTicket orderTicket = OrderTicket.builder()
                .order(savedOrder) // 참조
                .ticket(savedTicket) // 참조
                .createdAt(LocalDateTime.now())
                .purchase_price(150000)
                .build();
        OrderTicket savedOrderTicket = orderTicketRepository.save(orderTicket);
        assertThat(savedOrderTicket.getId()).isNotNull();
    }

}
