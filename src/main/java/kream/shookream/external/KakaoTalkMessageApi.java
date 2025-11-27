package kream.shookream.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class KakaoTalkMessageApi {
    private static final Random random = new Random();

    public void sendEventJoinMessage(String phoneNumber, List<String> eventName) {
        try {
            Thread.sleep(random.nextInt(500, 1500));
            log.info("카카오톡 알림 발송 완료 - 전화번호 : {}", phoneNumber);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("카카오톡 알림 발송중 인터럽트 발생", e);
        }
    }
}
