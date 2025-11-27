package kream.shookream.service.facade;

import kream.shookream.domain.Event;
import kream.shookream.domain.OrderTicket;
import kream.shookream.domain.Ticket;
import kream.shookream.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockManagerFacade {
    private final EventRepository eventRepository;

    // 락 획득 / 재고 조정 / 플러시
    public List<Long> prepareLockAndStockForOrder(List<Ticket> tickets) {
        // 1. Deadlock 방지를 위해 락을 걸 Event ID 목록을 정렬
        Set<Long> distinctEventIds = tickets.stream()
                .map(ticket -> ticket.getEvent().getId())
                .collect(Collectors.toSet());

        List<Long> sortedEventIds = distinctEventIds.stream()
                .sorted(Comparator.naturalOrder()) // ID 오름차순 정렬 강제
                .collect(Collectors.toList());

        // 2. 정렬된 순서대로 락 획득 및 재고 감소
        for (Long eventId : sortedEventIds) {
            Event eventWithLock = eventRepository.findWithPessimisticLockById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("이벤트 ID : " + eventId + " 를 찾을 수 없습니다."));

            // 안전을 위해 Ticket 내부의 Event 참조를 락 걸린 객체로 갱신
            tickets.stream()
                    .filter(ticket -> ticket.getEvent().getId().equals(eventId))
                    .forEach(ticket -> ticket.setEvent(eventWithLock));

            // 재고 감소 로직 실행
            eventWithLock.decreaseTicketStock();

            // 쓰기 지연 회피: UPDATE 쿼리를 즉시 DB에 반영하여 재고 감소를 강제
            eventRepository.flush();
        }

        return sortedEventIds;
    }

    /**
     * 주문 취소 시, 락 획득만 수행하여 영속성 컨텍스트에 안전한 상태로 가져옴
     */
    public void prepareLockForCancel(List<OrderTicket> orderTickets) {
        // 취소 대상 OrderTicket에서 Event ID 목록을 추출 및 정렬
        List<Long> eventIdsToLock = orderTickets.stream()
                .map(orderTicket -> orderTicket.getTicket().getEvent().getId())
                .distinct()
                .sorted(Comparator.naturalOrder()) // Deadlock 방지
                .collect(Collectors.toList());

        // 락 획득 (Event 상태 변경은 Order.cancel() 내부에서 Dirty Checking으로 처리됨)
        for (Long eventId : eventIdsToLock) {
            eventRepository.findWithPessimisticLockById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("아이디가 " + eventId + "인 이벤트를 찾을 수 없습니다"));
        }
    }
}
