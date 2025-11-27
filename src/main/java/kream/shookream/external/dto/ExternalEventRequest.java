package kream.shookream.external.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExternalEventRequest {
    private Long eventId;
    private Long memberId;
    private String eventName;
}
