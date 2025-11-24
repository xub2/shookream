package kream.shookream.service;

import kream.shookream.domain.*;
import kream.shookream.repository.EventRepository;
import kream.shookream.repository.MemberRepository;
import kream.shookream.repository.OrderRepository;
import kream.shookream.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Order createOrder(Long memberId, List<Long> ticketsIds) {
        // 접근할 멤버 찾아오기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        // 락을 걸 티켓들 조회
        List<Ticket> tickets = ticketRepository.findAllById(ticketsIds);

        // Event 엔티티에 락을 걸어야하는 이유?
        // 이벤트는 Ticket을 통해 접근해야함 -> Event에 락 걸어 조회

        if (tickets.size() != ticketsIds.size()) {
            throw new IllegalStateException("요청한 모든 티켓을 찾을 수 없습니다.");
        }

        //todo 여기서 데드락 문제 해결해야함
        Set<Long> distinctEventIds = tickets.stream()
                .map(ticket -> ticket.getEvent().getId())
                .collect(Collectors.toSet());

        List<Long> sortedEventIds = distinctEventIds.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        for (Long eventId : sortedEventIds) {
            Event eventWithLock = eventRepository.findWithPessimisticLockById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("이벤트 ID : " + eventId + " 를 찾을 수 없습니다."));

            // 💡 주의: 이 Event 객체가 tickets 리스트의 Ticket 객체 내부에 있는
            // 오래된 Event 객체 참조를 대체하지 않더라도,
            // JPA는 트랜잭션 내에서 ID가 같은 엔티티(eventWithLock)를 사용하여 Dirty Checking을 수행합니다.
            // 하지만 안전을 위해 tickets 리스트 내의 Event 참조를 갱신하는 것이 좋습니다.
            tickets.stream()
                    .filter(t -> t.getEvent().getId().equals(eventId))
                    .forEach(t -> t.setEvent(eventWithLock));
        }

        // 이 시점에 락 걸려있음 -> 안전하게 주문 생성
        Order newOrder = Order.createOrder(member, tickets);

        return orderRepository.save(newOrder);
    }

    @Transactional
    public void cancelOrder(Long orderId) {

        //todo 해당 order 조회시 연관된 티켓 다 가져와야함 -> fetch join 추가해야함 -> 해당 메서드 만들고 수정
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다"));

        // 락 필요한 이벤트(취소 대상) 조회 및 해당 이벤트 락 걸기 -> 수정이기 때문
        List<Long> eventIdsToLock = order.getOrderTickets().stream()
                .map(orderTicket -> orderTicket.getTicket())
                .map(ticket -> ticket.getEvent())
                .map(event -> event.getId())
                .distinct()
                .sorted() // 데드락 해결하기 위한 오름차순 정리
                .collect(Collectors.toList());


        for (Long eventId : eventIdsToLock) {
            eventRepository.findWithPessimisticLockById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("아이디가 " + eventId + "인 이벤트를 찾을 수 없습니다"));

            // 락 걸린 상태로 영속성 컨텍스트 올라옴 -> 비즈니스 메서드 안전하게 호출 가능(Order.cancel -> 재고 증가 로직)
        }

        order.cancel();

        // dirtyCheck 실행
    }
}
