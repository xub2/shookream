package kream.shookream.service;

import kream.shookream.config.IntegrationTest;
import kream.shookream.domain.*;
import kream.shookream.domain.embedded.EventTime;
import kream.shookream.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
@DisplayName("OrderService 통합 테스트 + 비관적 락")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired private EventRepository eventRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private SellerRepository sellerRepository;
    @Autowired private OrderRepository orderRepository;

    // 테스트 데이터 ID
    private Long memberId;
    private Long eventId;
    private Long ticketId;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화

        // 1. Venue + Seller
        Venue venue = venueRepository.save(Venue.builder()
                .venueName("홀")
                .capacity(10)
                .venuePhoneNumber("111")
                .build());

        Seller seller = sellerRepository.save(Seller.builder()
                .sellerName("사")
                .sellerEmail("a@b.com")
                .sellerType(SellerType.COMPANY)
                .sellerPhoneNumber("222")
                .sellerRegisterDate(LocalDateTime.now())
                .build());

        // 2. Member
        Member member = memberRepository.save(Member.builder()
                .email("test@user.com")
                .phoneNumber("010")
                .password("hash")
                .build());
        this.memberId = member.getId();

        // 3. Event (재고 1개로 설정)
        Event event = eventRepository.save(Event.builder()
                .venue(venue).seller(seller).eventName("공연")
                .eventType(EventType.CONCERT).eventTime(new EventTime(LocalDateTime.now(), LocalDateTime.now().plusHours(1)))
                .maxTicketCount(1) // 재고 1
                .build());
        this.eventId = event.getId();

        // 4. Ticket (Event와 연결)
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .event(event).seller(seller).seatInfo("A1")
                .ticketPrice(50000).status(TicketStatus.AVAILABLE)
                .build());
        this.ticketId = ticket.getId();
    }

    // 1. 주문 생성 (createOrder())
    @Test
    @DisplayName("주문 생성 성공 시_DB 상태가 일관되게 업데이트되어야 한다")
    void createOrderSuccessTest() {
        // given
        List<Long> ticketIds = Collections.singletonList(ticketId);

        // when
        Order createdOrder = orderService.createOrder(memberId, ticketIds);

        Order verifiedOrder = orderRepository.findWithOrderTicketsById(createdOrder.getId()).get();

        // then: DB 검증
        assertThat(createdOrder.getId()).isNotNull();

        // 1. Ticket 상태 검증 (AVAILABLE -> SOLDOUT)
        Ticket soldTicket = ticketRepository.findById(ticketId).get();
        assertThat(soldTicket.getStatus()).isEqualTo(TicketStatus.SOLDOUT);

        // 2. Event 재고 검증 (1 -> 0)
        Event eventAfterOrder = eventRepository.findById(eventId).get();
        assertThat(eventAfterOrder.getCurrentTicketStockCount()).isEqualTo(0);

        // 3. OrderTicket 검증 (주문 상세가 생성되었는지)
        assertThat(verifiedOrder.getOrderTickets()).hasSize(1);
    }

    @Test
    @DisplayName("재고 부족(0) 시_주문 생성에 실패해야 하며_DB 상태는 유지되어야 한다")
    void createOrderFailure_StockZero() {
        // given: 재고를 0으로 만들기 위해 한 번 주문을 먼저 진행
        orderService.createOrder(memberId, Collections.singletonList(ticketId));

        // when & then: 두 번째 주문 시도 시 예외 발생 예상
        assertThatThrownBy(() -> orderService.createOrder(memberId, Collections.singletonList(ticketId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 매진된 표입니다.");

        // DB 상태 확인 (롤백 또는 이전 상태 유지 확인)
        Event eventAfterFailure = eventRepository.findById(eventId).get();
        assertThat(eventAfterFailure.getCurrentTicketStockCount()).isEqualTo(0); // 0으로 유지
        assertThat(orderRepository.count()).isEqualTo(1); // 첫 주문만 성공하고, 두 번째 주문은 실패했으므로 Order는 1개여야 함
    }

    // 2. 주문 취소 테스트 (cancelOrder)

    @Test
    @DisplayName("주문 취소 성공 시_DB 상태가 재고 복구 상태로 업데이트되어야 한다")
    void cancelOrderSuccessTest() {
        // given: 주문을 먼저 생성하여 SOLDOUT 및 재고 0 상태를 만듦
        Order createdOrder = orderService.createOrder(memberId, Collections.singletonList(ticketId));
        Long orderId = createdOrder.getId();

        // when: 주문 취소
        orderService.cancelOrder(orderId);

        // then: DB 검증
        // 1. Order 상태 검증 (SUCCESS -> CANCELED)
        Order canceledOrder = orderRepository.findById(orderId).get();
        assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);

        // 2. Ticket 상태 검증 (SOLDOUT -> AVAILABLE)
        Ticket availableTicketAfterCancel = ticketRepository.findById(ticketId).get();
        assertThat(availableTicketAfterCancel.getStatus()).isEqualTo(TicketStatus.AVAILABLE);

        // 3. Event 재고 검증 (0 -> 1로 복구)
        Event eventAfterCancel = eventRepository.findById(eventId).get();
        assertThat(eventAfterCancel.getCurrentTicketStockCount()).isEqualTo(1);
    }
}