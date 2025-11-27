package kream.shookream.external;

import kream.shookream.external.dto.ExternalEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
@Slf4j
public class ExternalEventApi {

    private static final Random random = new Random();

    public ExternalEventResponse registerParticipant(List<Long> eventIds, Long memberId, List<String> eventNames) {
        try {
            Thread.sleep(random.nextInt(500, 1500));
            return ExternalEventResponse.builder()
                    .success(true)
                    .externalId(UUID.randomUUID().toString())
                    .errorMessage(null)
                    .build();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public ExternalEventResponse getParticipantInfo(Long eventId, Long memberId) {
        try {
            Thread.sleep(100);
            return ExternalEventResponse.builder()
                    .success(true)
                    .externalId(UUID.randomUUID().toString())
                    .errorMessage(null)
                    .build();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
