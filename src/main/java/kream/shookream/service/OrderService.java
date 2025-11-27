package kream.shookream.service;

import kream.shookream.domain.Event;
import kream.shookream.domain.Member;
import kream.shookream.domain.Order;
import kream.shookream.domain.Ticket;
import kream.shookream.external.ExternalEventApi;
import kream.shookream.external.KakaoTalkMessageApi;
import kream.shookream.external.dto.ExternalEventResponse;
import kream.shookream.external.event.EventJoinCompletedEvent;
import kream.shookream.repository.EventRepository;
import kream.shookream.repository.MemberRepository;
import kream.shookream.repository.OrderRepository;
import kream.shookream.repository.TicketRepository;
import kream.shookream.service.facade.StockManagerFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;

    private final StockManagerFacade stockManagerFacade;

    private final ExternalEventApi externalEventApi;
    private final KakaoTalkMessageApi kakaoTalkMessageApi;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(Long memberId, List<Long> ticketsIds) {
        // 접근할 멤버 찾아오기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        // 락을 걸 티켓들 조회
        // Event 엔티티에 락을 걸어야하는 이유? -> 외래키로 연결 되어 있기 때문 그래서 이벤트는 Ticket을 통해 접근해야함 -> Event에 락 걸어 조회
        List<Ticket> tickets = ticketRepository.findAllById(ticketsIds);
        if (tickets.size() != ticketsIds.size()) {
            throw new IllegalStateException("요청한 모든 티켓을 찾을 수 없습니다.");
        }

        // Facade 패턴 적용
        List<Long> sortedEventIds = stockManagerFacade.prepareLockAndStockForOrder(tickets);

        // 이 시점에 베타락 걸려있음 -> 안전하게 주문 생성
        Order newOrder = Order.createOrder(member, tickets);

        List<String> eventNameList = sortedEventIds.stream()
                .map(eventId -> eventRepository.findById(eventId).get())
                .map(event -> event.getEventName())
                .collect(Collectors.toList());

        // 이 시점에 상위 회사 (티켓 링크) API 호출 -> 해당 이벤트 참가 처리
        ExternalEventResponse response = externalEventApi.registerParticipant(
                sortedEventIds, memberId, eventNameList
        );

        if (!response.isSuccess()) {
            throw new RuntimeException("티켓 링크 API 호출 실패 : " + response.getErrorMessage());
        }

        // 여기에서 insert 문 실행
        Order savedOrder = orderRepository.save(newOrder);



        // 3. 카카오톡 알림 발송 (비동기 처리)
        eventPublisher.publishEvent(new EventJoinCompletedEvent(
                sortedEventIds,
                eventNameList,
                member.getPhoneNumber()
        ));

        return savedOrder;
    }

    @Transactional
    public void cancelOrder(Long orderId) {

        // N + 1 문제 해결
        Order order = orderRepository.findWithOrderDetailAndTicketById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));

        // 락 필요한 이벤트(취소 대상) 조회 및 해당 이벤트 락 걸기 -> 수정이기 때문
        stockManagerFacade.prepareLockForCancel(order.getOrderTickets());

        // 주문 취소 (취소 + 재고 복구) + dirtyCheck 실행
        order.cancel();

    }

    /**
     * 가상 시나리오 : 유저가 이벤트 예약시 DB에 좌석 정보 저장 -> 외부 API (결제 시스템 및 카카오톡 알림) 호출 해야함
     * 만약 위 로직들이 하나의 트랜잭션으로 묶이게 되면 외부 API 실패했다고 (예를 들면 중요하지 않은 알림 톡 API) 기존 예약 로직 까지 전부 실패하게 된다
     *
     * 1. DB커넥션 점유 시간 최소화 -> 트랜잭션 범위 조정
     * 2. 외부 API 장애 발생 시 예약 정보 유지 -> 후보정
     * 3. 카카오톡 알림 등은 트랜잭션에서 분리하여 독립적 시행
     *
     * 예약을 하면 외부 API 에서 예약 번호를 받아서 참자가 정보에 저장해둬야 한다
     */
}
