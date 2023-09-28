package entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassTime {

    private Subject subject = new Subject();
    private String time;
    private String dayOfTheWeek;
    private boolean preferable = false;
    private int prioritization;
}
