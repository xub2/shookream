package kream.shookream.external.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalEventResponse {
    private boolean success;
    private String externalId;
    private String message;
    private String errorMessage;
}
