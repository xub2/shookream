package kream.shookream.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import kream.shookream.domain.embedded.EventTime;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("도메인 비즈니스 로직 테스트")
public class OrderDomainTest {

    private Member testMember;
    private Venue testVenue;
    private Seller testSeller;
    private Event testEvent;
    private Ticket availableTicket;

    @BeforeEach
    void setUp() {
        // 1. 의존성 최소
         testMember = Member.builder()
                .email("email@naver.com")
                .phoneNumber("010-1414-1414")
                .password("비밀번호 4886")
                .build();

        testVenue = Venue.builder()
                .venueName("테스트홀")
                .capacity(100)
                .venuePhoneNumber("111")
                .build();

        testSeller = Seller.builder()
                .sellerName("테스트사")
                .sellerEmail("test@seller.com")
                .sellerType(SellerType.COMPANY)
                .sellerPhoneNumber("222")
                .sellerRegisterDate(LocalDateTime.now()).build();

        // 2. Event 객체 생성 (재고 1개)
        testEvent = Event.builder()
                .venue(testVenue)
                .seller(testSeller)
                .eventName("테스트 공연")
                .eventType(EventType.CONCERT)
                .eventTime(new EventTime(LocalDateTime.now(), LocalDateTime.now().plusHours(2)))
                .maxTicketCount(1) // ⭐️ 핵심: 재고를 1개로 설정
                .build();

        // 3. Ticket 객체 생성 (Event와 연결)
        availableTicket = Ticket.builder()
                .event(testEvent)
                .seller(testSeller)
                .seatInfo("A1")
                .ticketPrice(50000)
                .status(TicketStatus.AVAILABLE)
                .build();
    }

    // -----------------------------------------------------------
    // 1. 주문 생성 테스트 (createOrder)
    // -----------------------------------------------------------

    @Test
    @DisplayName("성공적인 주문 생성 시_티켓상태 SOLDOUT 및 재고 0으로 감소")
    void createOrderSuccessTest() {
        // given: 구매할 티켓 리스트 (1개)
        List<Ticket> ticketsToPurchase = Collections.singletonList(availableTicket);

        // when: 주문 생성 (createOrder 팩토리 메서드 호출)
        Order order = Order.createOrder(testMember, ticketsToPurchase);

        // then:
        // 1. Order 객체 상태 검증
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(order.getTotalOrderAmount()).isEqualTo(50000);
        assertThat(order.getOrderTickets()).hasSize(1);

        // 2. Ticket 객체 상태 검증 (SOLDOUT으로 변경되었는지)
        assertThat(availableTicket.getStatus()).isEqualTo(TicketStatus.SOLDOUT);

        // 3. Event 객체 재고 검증 (1 -> 0으로 감소했는지)
        assertThat(testEvent.getCurrentTicketStockCount()).isEqualTo(0);

        // 4. 양방향 매핑 검증 (OrderTicket -> Order)
        OrderTicket createdOrderTicket = order.getOrderTickets().get(0);
        assertThat(createdOrderTicket.getOrder()).isEqualTo(order);
    }

    @Test
    @DisplayName("이미 SOLDOUT 티켓으로 주문 시도 시_예외 발생")
    void createOrderFailure_SoldOutTicket() {
        // given: 티켓 상태를 강제로 SOLDOUT으로 변경
        availableTicket.sell();
        List<Ticket> ticketsToPurchase = Collections.singletonList(availableTicket);

        // when & then: 주문 생성 시 IllegalStateException 발생 예상
        assertThatThrownBy(() -> Order.createOrder(testMember, ticketsToPurchase))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 매진된 표입니다.");
    }

    // -----------------------------------------------------------
    // 2. 주문 취소 테스트 (cancel)
    // -----------------------------------------------------------

    @Test
    @DisplayName("주문 취소 시_주문상태 CANCELED, 티켓상태 AVAILABLE, 재고 1로 복구")
    void cancelOrderSuccessTest() {
        // given: 성공적으로 생성된 주문 (재고 0, 티켓 SOLDOUT 상태)
        Order order = Order.createOrder(testMember, Collections.singletonList(availableTicket));

        // when: 주문 취소
        order.cancel();

        // then:
        // 1. Order 객체 상태 검증
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);

        // 2. Ticket 객체 상태 검증 (AVAILABLE로 복구되었는지)
        assertThat(availableTicket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);

        // 3. Event 객체 재고 검증 (0 -> 1로 복구되었는지)
        assertThat(testEvent.getCurrentTicketStockCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 CANCELED 주문을 다시 취소 시도 시_예외 발생")
    void cancelOrderFailure_AlreadyCancelled() {
        // given: 주문 생성 후 바로 취소하여 CANCELED 상태로 만듦
        Order order = Order.createOrder(testMember, Collections.singletonList(availableTicket));
        order.cancel();

        // when & then: 다시 취소 시도 시 IllegalStateException 발생 예상
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("현재 주문은 취소할 수 없습니다.");
    }
}


