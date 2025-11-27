package kream.shookream.external.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class EventJoinCompletedEvent {
    private final List<Long> eventId;
    private final List<String> eventName;
    private final String phoneNumber;
}
