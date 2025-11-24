package kream.shookream;

import jakarta.transaction.Transactional;
import kream.shookream.domain.*;
import kream.shookream.domain.embedded.EventTime;
import kream.shookream.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {
    // 필요한 모든 Repository 주입
    private final VenueRepository venueRepository;
    private final SellerRepository sellerRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional // ⭐️ 데이터를 저장하는 트랜잭션 필요
    public void run(String... args) throws Exception {
        // [1. 의존성이 없는 엔티티부터 생성]

        // 1-1. Venue (장소)
        Venue mainVenue = venueRepository.save(
                Venue.builder().venueName("잠실 올림픽 주경기장").capacity(60000).venuePhoneNumber("02-1234-0000").build()
        );

        // 1-2. Seller (판매자/기획사)
        Seller mainSeller = sellerRepository.save(
                Seller.builder().sellerName("월드투어 기획사").sellerEmail("tour@world.com").sellerType(SellerType.COMPANY).sellerPhoneNumber("02-9999-0000").sellerRegisterDate(LocalDateTime.now()).build()
        );

        // 1-3. Member (테스트 사용자)
        memberRepository.save(
                Member.builder().email("user@test.com").phoneNumber("010-1234-5678").password("hashedpassword").build()
        );

        // [2. Event 생성 (Venue, Seller 의존)]
        EventTime eventTime = new EventTime(
                LocalDateTime.of(2026, 10, 15, 19, 0),
                LocalDateTime.of(2026, 10, 15, 22, 0)
        );

        Event mainEvent = Event.builder()
                .venue(mainVenue)
                .seller(mainSeller)
                .eventName("2026 월드투어 서울 파이널")
                .eventType(EventType.CONCERT)
                .eventTime(eventTime)
                .maxTicketCount(500) // 총 500개 재고
                .build();
        eventRepository.save(mainEvent);

        // [3. Ticket 생성 (Event 의존)]

        // 500개의 티켓 중 5개를 AVAILABLE 상태로 생성 (테스트용)
        for (int i = 1; i <= 5; i++) {
            Ticket ticket = Ticket.builder()
                    .event(mainEvent)
                    .seller(mainSeller)
                    .seatInfo(String.format("A구역 1층 %d열", i))
                    .ticketPrice(120000)
                    .status(TicketStatus.AVAILABLE)
                    .build();
            ticketRepository.save(ticket);
        }

        System.out.println("=== 테스트 데이터 초기화 완료: 1개 이벤트, 5개 티켓 생성 ===");
    }
}
