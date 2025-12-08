package kream.shookream.service;

import kream.shookream.config.IntegrationTest;
import kream.shookream.domain.Member;
import kream.shookream.domain.Ticket;
import kream.shookream.domain.TicketStatus;
import kream.shookream.external.ExternalEventApi;
import kream.shookream.external.KakaoTalkMessageApi;
import kream.shookream.external.dto.ExternalEventResponse;
import kream.shookream.external.event.EventJoinCompletedEvent;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Mock 없이 비동기 이벤트 처리를 테스트하는 클래스
 */
@IntegrationTest
@EnableAsync // 비동기 처리를 활성화
@Slf4j
class AsyncEventTest {

    @Autowired private OrderService orderService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TicketRepository ticketRepository;

    // ❌ MockBean 대신, TestConfiguration에서 생성된 실제 Bean을 주입받습니다.
    @Autowired private KakaoTalkMessageApi kakaoTalkMessageApi;

    // 외부 API는 테스트 속도를 위해 Mock으로 유지
    @Autowired private ExternalEventApi externalEventApi;

    private Member testMember;
    private List<Long> initialTicketIds;
    private String mainThreadName; // 메인 테스트 스레드 이름 저장

    // --- 1. 테스트용 Dummy KakaoTalk API 구현체 ---
    static class TestableKakaoTalkApi extends KakaoTalkMessageApi {
        private final long delayMs = 1000;
        private volatile boolean wasCalled = false;
        private volatile String callingThreadName = null;

        @Override
        public void sendEventJoinMessage(String phoneNumber, List<String> eventName) {
            this.callingThreadName = Thread.currentThread().getName();
            try {
                System.out.println("🤖 Dummy KakaoTalk API 호출 시작 (1000ms 지연). 스레드: " + callingThreadName);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.wasCalled = true;
        }

        // OrderService.java의 EventPublisher 호출 시 sendEventJoinMessage를 사용하지 않아
        // OrderServiceBeforeRefactor.java에 맞게 sendMessage도 구현했습니다.
//        @Override
        public void sendMessage(String phoneNumber, List<String> eventName, List<Long> eventIds) {
            this.sendEventJoinMessage(phoneNumber, eventName);
        }

        public boolean wasCalled() { return wasCalled; }
        public String getCallingThreadName() { return callingThreadName; }
        public void reset() { this.wasCalled = false; this.callingThreadName = null; }
    }

    // --- 2. 테스트 환경 Bean 설정 ---
    @TestConfiguration
    static class TestApiConfig {
        // Dummy KakaoTalk API를 실제 KakaoTalkMessageApi Bean으로 등록
        @Primary
        @Bean
        public KakaoTalkMessageApi kakaoTalkMessageApi() {
            return new TestableKakaoTalkApi();
        }

        // ExternalEventApi는 Mock이 아닌 Test Bean으로 등록 (지연 시간 0ms)
        @Primary
        @Bean
        public ExternalEventApi externalEventApi() {
            return new ExternalEventApi() {
                @Override
                public ExternalEventResponse registerParticipant(List<Long> eventIds, Long memberId, List<String> eventNameList) {
                    return ExternalEventResponse.builder().success(true).build();
                }

                @Override
                public ExternalEventResponse getParticipantInfo(Long eventId, Long memberId) {
                    return ExternalEventResponse.builder().success(true).build();
                }
            };
        }
    }


    @BeforeEach
    @Transactional // @BeforeEach에 트랜잭션을 걸어 데이터 롤백을 보장합니다.
    void setUp() {
        mainThreadName = Thread.currentThread().getName();

        // Dummy API 초기화
        ((TestableKakaoTalkApi) kakaoTalkMessageApi).reset();

        // 테스트 데이터 설정 (실제 DB에 저장)
        testMember = Member.builder().phoneNumber("010-1234-5678").build();
        memberRepository.save(testMember);

        List<Long> TICKET_IDS = Arrays.asList(101L, 102L);
        List<Ticket> ticketsToSave = TICKET_IDS.stream()
                .map(id -> Ticket.builder().seatInfo("A" + id).ticketPrice(10000).status(TicketStatus.AVAILABLE).build())
                .collect(Collectors.toList());
        List<Ticket> savedTickets = ticketRepository.saveAll(ticketsToSave);
        initialTicketIds = savedTickets.stream().map(Ticket::getId).collect(Collectors.toList());
    }

    @Test
    @DisplayName("카카오톡 알림 발송은 비동기 스레드에서 실행되어야 한다")
    void kakao_message_should_be_processed_asynchronously() {
        // given: TestableKakaoTalkApi에 1000ms 지연 설정 완료

        // when: OrderService 호출
        long start = System.currentTimeMillis();
        orderService.createOrder(testMember.getId(), initialTicketIds);
        long duration = System.currentTimeMillis() - start;

        // then 1: createOrder 메서드는 1000ms 지연을 기다리지 않고 빠르게 반환되어야 한다. (비동기 증명)
        assertThat(duration)
                .as("createOrder는 1000ms 지연을 포함한 이벤트 대기 없이 500ms 이내에 반환되어야 함")
                .isLessThan(500);

        // then 2: Dummy API의 상태 변화를 Awaitility로 확인
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    TestableKakaoTalkApi dummyApi = (TestableKakaoTalkApi) kakaoTalkMessageApi;

                    // 1. 호출 완료 확인
                    assertThat(dummyApi.wasCalled())
                            .as("비동기 API가 호출되어 상태가 변경되었는지 확인")
                            .isTrue();

                    // 2. 비동기 스레드에서 호출되었는지 확인
                    assertThat(dummyApi.getCallingThreadName())
                            .as("API 호출은 메인 스레드가 아닌, 다른 비동기 스레드에서 발생해야 함")
                            .isNotEqualTo(mainThreadName);
                });

        log.info("최종 주문 생성 시간: {}ms", duration);
    }
}