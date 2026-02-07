package ru.practicum.aggregator;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Component
public class SimilarityAggregator {
    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";
    private static final String EVENTS_SIMILARITY_TOPIC = "stats.events-similarity.v1";

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public SimilarityAggregator(KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = USER_ACTIONS_TOPIC)
    public void handleUserAction(UserActionAvro action) {
        if (action == null) {
            return;
        }
        synchronized (this) {
            long userId = action.userId();
            long eventId = action.eventId();
            double newWeight = weightForAction(action.actionType());

            double oldWeight = eventUserWeights
                    .computeIfAbsent(eventId, id -> new HashMap<>())
                    .getOrDefault(userId, 0.0);

            if (newWeight <= oldWeight) {
                return;
            }

            eventUserWeights.get(eventId).put(userId, newWeight);
            userEventWeights.computeIfAbsent(userId, id -> new HashMap<>()).put(eventId, newWeight);

            double deltaWeight = newWeight - oldWeight;
            eventWeightSums.put(eventId, eventWeightSums.getOrDefault(eventId, 0.0) + deltaWeight);

            Map<Long, Double> userEvents = userEventWeights.getOrDefault(userId, Map.of());
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
    }

    private void updateMinWeightSum(long eventA, long eventB, double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        Map<Long, Double> inner = minWeightsSums.computeIfAbsent(first, id -> new HashMap<>());
        inner.put(second, inner.getOrDefault(second, 0.0) + delta);
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
        kafkaTemplate.send(EVENTS_SIMILARITY_TOPIC, first + ":" + second, similarity);
    }

    private double weightForAction(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}