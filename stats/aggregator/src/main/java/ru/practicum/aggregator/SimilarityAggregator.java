package ru.practicum.aggregator;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class SimilarityAggregator {
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    private final String eventsSimilarityTopic;
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public SimilarityAggregator(
            KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate,
            @Value("${app.kafka.topics.events-similarity}") String eventsSimilarityTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventsSimilarityTopic = eventsSimilarityTopic;
    }

    @KafkaListener(topics = "${app.kafka.topics.user-actions}")
    public void handleUserAction(UserActionAvro action) {
        if (action == null) {
            return;
        }
        long userId = action.userId();
        long eventId = action.eventId();
        double newWeight = weightForAction(action.actionType());

        Map<Long, Double> userEvents = userEventWeights.computeIfAbsent(userId, id -> new ConcurrentHashMap<>());
        double oldWeight = userEvents.getOrDefault(eventId, 0.0);

        if (newWeight <= oldWeight) {
            return;
        }

        userEvents.put(eventId, newWeight);
        double deltaWeight = newWeight - oldWeight;
        eventWeightSums.merge(eventId, deltaWeight, Double::sum);

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) {
                continue;
            }
            double otherWeight = entry.getValue();
            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double deltaMin = newMin - oldMin;

            updateMinWeightSum(eventId, otherEventId, deltaMin);
            sendSimilarity(eventId, otherEventId, action);
        }
    }

    private void updateMinWeightSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        Map<Long, Double> inner = minWeightsSums.computeIfAbsent(first, id -> new ConcurrentHashMap<>());
        inner.merge(second, delta, Double::sum);
    }

    private double getMinWeightSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }

    private void sendSimilarity(long eventId, long otherEventId, UserActionAvro action) {
        double sumA = eventWeightSums.getOrDefault(eventId, 0.0);
        double sumB = eventWeightSums.getOrDefault(otherEventId, 0.0);
        if (sumA <= 0 || sumB <= 0) {
            return;
        }
        double minSum = getMinWeightSum(eventId, otherEventId);
        double denominator = Math.sqrt(sumA) * Math.sqrt(sumB);
        if (denominator == 0.0) {
            return;
        }
        double score = minSum / denominator;
        long first = Math.min(eventId, otherEventId);
        long second = Math.max(eventId, otherEventId);

        EventSimilarityAvro similarity = new EventSimilarityAvro(first, second, score, action.timestamp());
        kafkaTemplate.send(eventsSimilarityTopic, first + ":" + second, similarity);
    }

    private double weightForAction(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}