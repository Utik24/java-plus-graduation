package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.StatisticsGetResponseDto;
import ru.practicum.dto.StatisticsPostResponseDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StatsClient {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public StatsClient(RestTemplateBuilder restTemplateBuilder,
                       DiscoveryClient discoveryClient,
                       @Value("${stats.service-id:stats-server}") String statsServiceId) {
        this.restTemplate = restTemplateBuilder.build();
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = createRetryTemplate();
    }

    public List<StatisticsGetResponseDto> getStats(LocalDateTime startTime, LocalDateTime endTime,
                                                   @Nullable String[] uris, @Nullable Boolean unique) {
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

    public ResponseEntity<String> postHit(StatisticsPostResponseDto hit) {
        return makeAndSendPostHitRequest(HttpMethod.POST, "/hit", null, hit);
    }


    private <T> List<StatisticsGetResponseDto> makeAndSendGetStatsRequest(HttpMethod method, String path,
                                                                          @Nullable Map<String, Object> parameters,
                                                                          @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<List<StatisticsGetResponseDto>> ewmServerResponse;
        try {
            String uriTemplate = makeUriString(path);
            if (parameters != null) {
                ewmServerResponse = restTemplate.exchange(uriTemplate, method, requestEntity,
                        new ParameterizedTypeReference<List<StatisticsGetResponseDto>>() {
                        }, parameters);
            } else {
                ewmServerResponse = restTemplate.exchange(uriTemplate, method, requestEntity,
                        new ParameterizedTypeReference<List<StatisticsGetResponseDto>>() {
                        });
            }
        } catch (HttpStatusCodeException e) {
            return null;
        }
        return ewmServerResponse.getBody();
    }

    private <T> ResponseEntity<String> makeAndSendPostHitRequest(HttpMethod method, String path,
                                                                 @Nullable Map<String, Object> parameters,
                                                                 @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<String> ewmServerResponse;
        try {
            String uriTemplate = makeUriString(path);
            if (parameters != null) {
                ewmServerResponse = restTemplate.exchange(uriTemplate, method, requestEntity, String.class, parameters);
            }
            else{
                ewmServerResponse = restTemplate.exchange(uriTemplate, method, requestEntity, String.class);
            }
        } catch (HttpStatusCodeException e) {
            return null;
        }
        return ewmServerResponse;
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    public Map<Long, Long> getMapIdViews(Collection<Long> eventsId) {
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
        List<StatisticsGetResponseDto> statisticsList = getStats(LocalDateTime.of(1970, 1, 1, 1, 1), LocalDateTime.now(), uriArray, true);
        if (statisticsList == null || statisticsList.isEmpty()) { //если нет статистики по эндпоинтам, возвращаем мапу с нулевыми просмотрами
            return eventsId.stream()
                    .collect(Collectors.toMap(e -> e, e -> 0L));
        }
        /*превращаем список EndpointStats в мапу <id события, кол-во просмотров>*/
        return statisticsList.stream().collect(Collectors.toMap(e -> {
                    String[] splitUri = e.getUri().split("/"); //делим URI /events/1
                    Arrays.asList(splitUri).forEach(s -> System.out.println("idViewsMap + elements+///+ " + s));
                    return Long.valueOf(splitUri[splitUri.length - 1]); //берем последний элемент разбитой строки - это id
                },
                StatisticsGetResponseDto::getHits));
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient.getInstances(statsServiceId).getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private String makeUriString(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return "http://" + instance.getHost() + ":" + instance.getPort() + path;
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        template.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);
        return template;
    }
}