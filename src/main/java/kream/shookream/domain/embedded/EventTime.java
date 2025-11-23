package kream.shookream.domain.embedded;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventTime {

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public String getRunningTime() {
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            return "";
        }

        Duration duration = Duration.between(startTime, endTime);

        long totalMinutes = duration.toMinutes();

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        } else {
            return minutes + "분";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventTime eventTime = (EventTime) o;
        return Objects.equals(getStartTime(), eventTime.getStartTime()) && Objects.equals(getEndTime(), eventTime.getEndTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartTime(), getEndTime());
    }
}
