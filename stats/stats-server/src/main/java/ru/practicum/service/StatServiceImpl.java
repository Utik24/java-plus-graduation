package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.StatisticsGetResponseDto;
import ru.practicum.dto.StatisticsPostResponseDto;
import ru.practicum.mappers.StatMapper;
import ru.practicum.model.Statistics;
import ru.practicum.repository.StatisticsRepository;
import ru.practicum.repository.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {
    private final StatisticsRepository statisticsRepository;

    @Transactional
    @Override
    public StatisticsPostResponseDto hit(StatisticsPostResponseDto statisticsPostResponseDto) {
        Statistics statistics = StatMapper.fromDto(statisticsPostResponseDto);
        return StatMapper.toDto(statisticsRepository.save(statistics));
    }

    @Transactional
    @Override
    public List<StatisticsGetResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new RuntimeException("время начала не может быть поздне, чем  время конца выборки");
        }
        List<ViewStats> rows = switch ((unique ? 2 : 0) + ((uris != null && !uris.isEmpty()) ? 1 : 0)) {
            case 0 -> statisticsRepository.findStats(start, end);
            case 1 -> statisticsRepository.findStatsByUris(start, end, uris);
            case 2 -> statisticsRepository.findUniqueStats(start, end);
            case 3 -> statisticsRepository.findUniqueStatsByUris(start, end, uris);
            default -> throw new RuntimeException();
        };

        return rows.stream()
                .map(v -> new StatisticsGetResponseDto(v.getApp(), v.getUri(), v.getHits()))
                .toList();
    }


}
