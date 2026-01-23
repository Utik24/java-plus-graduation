package ru.practicum.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.dto.StatisticsPostResponseDto;
import ru.practicum.model.Statistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StatMapper {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public static StatisticsPostResponseDto toDto(Statistics statistics) {
        return StatisticsPostResponseDto.builder()
                .ip(statistics.getIp())
                .app(statistics.getApp())
                .uri(statistics.getUri())
                .timestamp(statistics.getTimestamp().format(TIME_FORMAT))
                .build();
    }

    public static Statistics fromDto(StatisticsPostResponseDto statisticsPostResponseDto) {
        return Statistics.builder()
                .uri(statisticsPostResponseDto.getUri())
                .app(statisticsPostResponseDto.getApp())
                .ip(statisticsPostResponseDto.getIp())
                .timestamp(LocalDateTime.parse(statisticsPostResponseDto.getTimestamp(), TIME_FORMAT))
                .build();
    }

}
