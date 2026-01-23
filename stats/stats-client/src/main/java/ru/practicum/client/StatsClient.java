package ru.practicum.client;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.dto.StatisticsGetResponseDto;
import ru.practicum.dto.StatisticsPostResponseDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StatsClient {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        private static final String SERVER_URL = "http://localhost:9090";
    private static final String SERVER_URL = "http://stats-server:9090";
    private static final RestTemplate rest;

    static {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        rest = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(SERVER_URL))
                .build();
    }

    public static List<StatisticsGetResponseDto> getStats(LocalDateTime startTime, LocalDateTime endTime, @Nullable String[] uris, @Nullable Boolean unique) {
        String startString = startTime.format(TIME_FORMAT);
        String endString = endTime.format(TIME_FORMAT);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", startString);
        parameters.put("end", endString);

        StringBuilder sb = new StringBuilder();
        sb.append("/stats?start={start}&end={end}");
        if (uris != null) {
            parameters.put("uris", uris);
            sb.append("&uris={uris}");
        }
        if (unique != null) {
            parameters.put("unique", unique);
            sb.append("&unique={unique}");
        }
        return makeAndSendGetStatsRequest(HttpMethod.GET, sb.toString(), parameters, null);
    }

    public static ResponseEntity<String> postHit(StatisticsPostResponseDto hit) {
        ResponseEntity<String> responseEntity = makeAndSendPostHitRequest(HttpMethod.POST, "/hit", null, hit);
        return responseEntity;
    }


    private static <T> List<StatisticsGetResponseDto> makeAndSendGetStatsRequest(HttpMethod method, String path, @Nullable Map<String, Object> parameters, @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<List<StatisticsGetResponseDto>> ewmServerResponse;
        try {
            if (parameters != null) {
                ewmServerResponse = rest.exchange(path, method, requestEntity, new ParameterizedTypeReference<List<StatisticsGetResponseDto>>() {
                }, parameters);
            } else {
                ewmServerResponse = rest.exchange(path, method, requestEntity, new ParameterizedTypeReference<List<StatisticsGetResponseDto>>() {
                });
            }
        } catch (HttpStatusCodeException e) {
            return null;
        }
        return ewmServerResponse.getBody();
    }

    private static <T> ResponseEntity<String> makeAndSendPostHitRequest(HttpMethod method, String path, @Nullable Map<String, Object> parameters, @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<String> ewmServerResponse;
        try {
            if (parameters != null) {
                ewmServerResponse = rest.exchange(path, method, requestEntity, String.class, parameters);
            } else {
                ewmServerResponse = rest.exchange(path, method, requestEntity, String.class);
            }
        } catch (HttpStatusCodeException e) {
            return null;
        }
        return ewmServerResponse;
    }

    private static HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    public static Map<Long, Long> getMapIdViews(Collection<Long> eventsId) {
        if (eventsId == null || eventsId.isEmpty()) {
            return new HashMap<>();
        }
        /*составляем список URI событий из подборки*/
        List<String> eventUris = eventsId.stream()
                .map(i -> "/events/" + i)
                .collect(Collectors.toList()); //преобразовали список событий в список URI

        String[] uriArray = new String[eventUris.size()]; //создали массив строк
        eventUris.toArray(uriArray); //заполнили массив строками из списка URI

        /*запрашиваем у клиента статистики данные по нужным URI*/
        List<StatisticsGetResponseDto> statisticsList = getStats(LocalDateTime.of(1970, 01, 01, 01, 01), LocalDateTime.now(), uriArray, true);

        if (statisticsList == null || statisticsList.isEmpty()) { //если нет статистики по эндпоинтам, возвращаем мапу с нулевыми просмотрами
            return eventsId.stream()
                    .collect(Collectors.toMap(e -> e, e -> 0L));
        }
        /*превращаем список EndpointStats в мапу <id события, кол-во просмотров>*/
        Map<Long, Long> idViewsMap = statisticsList.stream()
                .collect(Collectors.toMap(e -> {
                            String[] splitUri = e.getUri().split("/"); //делим URI /events/1
                            Arrays.asList(splitUri).forEach(s -> System.out.println("idViewsMap + elements+///+ " + s));
                            return Long.valueOf(splitUri[splitUri.length - 1]); //берем последний элемент разбитой строки - это id
                        },
                        StatisticsGetResponseDto::getHits));
        return idViewsMap;
    }
}