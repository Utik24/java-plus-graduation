package ru.practicum.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.model.Statistics;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsPostResponseDto {
    String app;
    String uri;
    String ip;
    String timestamp;

    public static StatisticsPostResponseDto toStatisticsPostResponseDto(Statistics statistics) {
        return new StatisticsPostResponseDto(statistics.getApp(), statistics.getUri(), statistics.getIp(), statistics.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
