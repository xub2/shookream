package kream.shookream.service;

import kream.shookream.config.IntegrationTest;
import kream.shookream.domain.Member;
import kream.shookream.domain.Ticket;
import kream.shookream.domain.TicketStatus;
import kream.shookream.external.ExternalEventApi;
import kream.shookream.external.dto.ExternalEventResponse;
import kream.shookream.repository.MemberRepository;
import kream.shookream.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@IntegrationTest
@Slf4j
public class OrderServiceTransactionIntegrationTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderServiceBeforeRefactor orderServiceBeforeRefactor;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TicketRepository ticketRepository;

    // 테스트 설정 변수
    private final int THREAD_COUNT = 50; // 동시 요청 수
    private final List<Long> TICKET_IDS = Arrays.asList(101L, 102L);
    private Member testMember;
    private List<Long> initialTicketIds; // 실제 DB에 저장된 Ticket ID

    // 테스트 환경에서만 동작하는 외부 API 지연 설정
    @TestConfiguration
    static class TestApiConfig {
        @Primary
        @Bean
        public ExternalEventApi externalEventApi() {
            return new ExternalEventApi() {
                @Override
                public ExternalEventResponse registerParticipant(
                        List<Long> eventIds, Long memberId, List<String> eventNameList) {

                    // 외부 API 호출 지연 시간 시뮬레이션 (100ms)
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return ExternalEventResponse.builder()
                                .success(false)
                                .externalId("API_INTERRUPTED")
                                .message("API Call Interrupted")
                                .errorMessage("Thread was interrupted during sleep.")
                                .build();
                    }

                    return ExternalEventResponse.builder()
                            .success(true)
                            .externalId(null)
                            .message(null)
                            .errorMessage(null)
                            .build();
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        // 1. 테스트 멤버 저장
        // ID는 JPA가 자동 생성
        testMember = Member.builder().phoneNumber("010-1234-5678").build();
        memberRepository.save(testMember);

        // 2. 테스트 티켓 저장 (ID를 할당하지 않고 저장)
        List<Ticket> ticketsToSave = Arrays.asList(
                Ticket.builder()
                        .event(null) // Event 객체는 필요하지만, 여기서는 단순화를 위해 null 또는 임시 Event 객체 사용
                        .seller(null)
                        .seatInfo("A1")
                        .ticketPrice(10000)
                        .status(TicketStatus.AVAILABLE)
                        .build(),
                Ticket.builder()
                        .event(null)
                        .seller(null)
                        .seatInfo("A2")
                        .ticketPrice(10000)
                        .status(TicketStatus.AVAILABLE)
                        .build()
        );

        // saveAll을 호출하면, 반환되는 List에는 DB가 할당한 실제 ID가 포함됩니다.
        List<Ticket> savedTickets = ticketRepository.saveAll(ticketsToSave);

        // 3. DB가 할당한 실제 ID 목록을 테스트 변수에 저장
        initialTicketIds = savedTickets.stream()
                .map(Ticket::getId)
                .collect(Collectors.toList());

        // TICKET_IDS는 이제 더 이상 사용하지 않고, initialTicketIds를 사용합니다.
    }

    // --- 1. 리팩토링된 OrderService의 TPS 측정 (트랜잭션 분리) ---
    @Test
    @DisplayName("리팩토링된 OrderService (DB 커밋 후 API 호출) TPS 측정")
    void measure_tps_for_refactored_service() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    // 실제 DB에 저장된 멤버 ID와 티켓 ID를 사용
                    orderService.createOrder(testMember.getId(), initialTicketIds);
                } catch (Exception e) {
                    // 동시성 제어 실패 (재고 락 실패) 또는 API 호출 실패 시 예외가 발생할 수 있습니다.
                    // System.err.println("주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        long endTime = System.currentTimeMillis();

        // then
        long duration = endTime - startTime;
        double tps = (double) THREAD_COUNT / (duration / 1000.0);

        System.out.println("--- 리팩토링된 OrderService 결과 (트랜잭션 분리) ---");
        System.out.println("총 요청 수: " + THREAD_COUNT);
        System.out.println("총 소요 시간 (ms): " + duration);
        System.out.printf("계산된 TPS: %.2f\n", tps);
    }

    // --- 2. 리팩토링 이전 OrderService의 TPS 측정 (단일 트랜잭션) ---
    @Test
    @DisplayName("리팩토링 이전 OrderService (단일 트랜잭션) TPS 측정")
    void measure_tps_for_original_service() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // when
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    // 실제 DB에 저장된 멤버 ID와 티켓 ID를 사용
                    orderServiceBeforeRefactor.createOrder(testMember.getId(), initialTicketIds);
                } catch (Exception e) {
                    // 동시성 제어 실패 또는 API 호출 실패 시 예외가 발생할 수 있습니다.
                    // System.err.println("주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        long endTime = System.currentTimeMillis();

        // then
        long duration = endTime - startTime;
        double tps = (double) THREAD_COUNT / (duration / 1000.0);

        System.out.println("\n--- 리팩토링 이전 OrderService 결과 (단일 트랜잭션) ---");
        System.out.println("총 요청 수: " + THREAD_COUNT);
        System.out.println("총 소요 시간 (ms): " + duration);
        System.out.printf("계산된 TPS: %.2f\n", tps);
    }
}
