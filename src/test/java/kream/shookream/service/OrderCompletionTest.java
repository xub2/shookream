package kream.shookream.service;

import kream.shookream.config.IntegrationTest;
import kream.shookream.domain.*;
import kream.shookream.external.ExternalEventApi;
import kream.shookream.external.KakaoTalkMessageApi;
import kream.shookream.external.dto.ExternalEventResponse;
import kream.shookream.repository.EventRepository;
import kream.shookream.repository.MemberRepository;
import kream.shookream.repository.OrderRepository;
import kream.shookream.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@IntegrationTest
@EnableAsync // 비동기 이벤트 리스너를 실행하기 위해 필요
@Slf4j
class OrderCompletionTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private EventRepository eventRepository;

    // 외부 연동 및 알림 서비스는 Mock 처리
    @MockBean private ExternalEventApi externalEventApi;
    @MockBean private KakaoTalkMessageApi kakaoTalkMessageApi;

    private Member testMember;
    private List<Long> initialTicketIds;

    @BeforeEach
    @Transactional
    void setUp() {
        // 0. 테스트용 Event 객체를 먼저 생성하고 저장합니다.
        Event testEvent = Event.builder()
                .eventName("알림톡 테스트 이벤트")
                .maxTicketCount(500)
                .build();
        eventRepository.save(testEvent);

        // 1. 테스트 멤버 저장
        testMember = Member.builder().phoneNumber("010-1234-5678").build();
        memberRepository.save(testMember);

        // 2. 테스트 티켓 저장 (Event 객체 연결)
        List<Ticket> ticketsToSave = Arrays.asList(
                Ticket.builder()
                        .event(testEvent) // 👈 Event 객체 주입
                        .seller(null)
                        .seatInfo("A1")
                        .ticketPrice(10000)
                        .status(TicketStatus.AVAILABLE)
                        .build(),
                Ticket.builder()
                        .event(testEvent) // 👈 Event 객체 주입
                        .seller(null)
                        .seatInfo("A2")
                        .ticketPrice(10000)
                        .status(TicketStatus.AVAILABLE)
                        .build()
        );

        List<Ticket> savedTickets = ticketRepository.saveAll(ticketsToSave);
        initialTicketIds = savedTickets.stream().map(Ticket::getId).collect(Collectors.toList());
    }

    // --- 1. 주문 성공 -> 카카오톡 알림 실패 -> 주문은 성공 ---
    @Test
    @DisplayName("1. 알림톡 발송 실패 시에도 주문은 최종적으로 성공 상태를 유지해야 한다.")
    void order_should_succeed_despite_kakao_failure() throws InterruptedException {
        // given
        // 카카오톡 API 호출 시 강제로 예외 발생 설정 (비동기 리스너 내부에서 발생)
        doThrow(new RuntimeException("카카오톡 서버 장애")).when(kakaoTalkMessageApi).sendEventJoinMessage(any(), any());

        // 외부 API는 성공하도록 설정 (외부 서비스 참여는 성공해야 주문이 유지됨)
        given(externalEventApi.registerParticipant(any(), any(), any()))
                .willReturn(ExternalEventResponse.builder().success(true).build());

        // when
        Order resultOrder = orderService.createOrder(testMember.getId(), initialTicketIds);

        // then 1: 주문 서비스 메서드는 즉시 성공적으로 종료되어야 한다.
        assertThat(resultOrder).isNotNull();

        // then 2: 데이터베이스에서 주문이 성공적으로 저장되었는지 확인한다.
        Optional<Order> savedOrder = orderRepository.findById(resultOrder.getId());
        assertThat(savedOrder).isPresent();
        // 주문 상태 검증 (예: OrderStatus.COMPLETED)
        // assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // then 3: 카카오톡 API가 호출은 시도되었는지 확인 (비동기 리스너 호출 확인)
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    // 최소 한 번 호출 시도는 있었음을 검증 (실제 예외는 비동기 스레드에서 처리됨)
                    verify(kakaoTalkMessageApi, timeout(2000)).sendEventJoinMessage(any(), any());
                });

        log.info("테스트 1 완료: 주문 ID {}는 성공적으로 DB에 저장되었습니다.", resultOrder.getId());
    }

    // --- 2. 주문 성공 -> 카카오톡 알림 성공 ---
    @Test
    @DisplayName("2. 외부 API 및 알림톡 발송 모두 성공 시 주문이 완료되어야 한다.")
    void order_and_kakao_should_succeed() throws InterruptedException {
        // given
        // 카카오톡 API 호출은 성공하도록 설정 (실제 로직은 성공적으로 통과)
        // doNothing()을 사용하거나, Mock의 기본 동작을 이용 (void 메서드의 기본은 doNothing)

        // 외부 API는 성공하도록 설정
        given(externalEventApi.registerParticipant(any(), any(), any()))
                .willReturn(ExternalEventResponse.builder().success(true).build());

        // when
        Order resultOrder = orderService.createOrder(testMember.getId(), initialTicketIds);

        // then 1: 주문 서비스 메서드는 즉시 성공적으로 종료되어야 한다.
        assertThat(resultOrder).isNotNull();

        // then 2: 데이터베이스에서 주문이 성공적으로 저장되었는지 확인한다.
        Optional<Order> savedOrder = orderRepository.findById(resultOrder.getId());
        assertThat(savedOrder).isPresent();

        // then 3: 카카오톡 API가 비동기로 호출되었는지 확인
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    // 성공적으로 호출되었음을 검증
                    verify(kakaoTalkMessageApi, timeout(2000)).sendEventJoinMessage(any(), any());
                });

        log.info("테스트 2 완료: 주문 ID {}가 성공적으로 저장되고 카카오톡이 발송되었습니다.", resultOrder.getId());
    }
}