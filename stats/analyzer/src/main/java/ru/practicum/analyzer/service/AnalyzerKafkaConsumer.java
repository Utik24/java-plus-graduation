package ru.practicum.analyzer.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventPairId;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.model.UserEventId;
import ru.practicum.analyzer.model.UserEventInteractionEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class AnalyzerKafkaConsumer {
    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";
    private static final String EVENTS_SIMILARITY_TOPIC = "stats.events-similarity.v1";

    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    public AnalyzerKafkaConsumer(UserEventInteractionRepository interactionRepository,
                                 EventSimilarityRepository similarityRepository) {
        this.interactionRepository = interactionRepository;
        this.similarityRepository = similarityRepository;
    }

    @KafkaListener(topics = USER_ACTIONS_TOPIC, containerFactory = "userActionKafkaListenerContainerFactory")
    @Transactional
    public void handleUserAction(UserActionAvro action) {
        if (action == null) {
            return;
        }
        double newWeight = weightForAction(action.actionType());
        UserEventId id = new UserEventId(action.userId(), action.eventId());
        UserEventInteractionEntity entity = interactionRepository.findById(id).orElse(null);
        if (entity != null && entity.getWeight() >= newWeight) {
            return;
        }
        if (entity == null) {
            entity = new UserEventInteractionEntity(id, newWeight, action.timestamp());
        } else {
            entity.setWeight(newWeight);
            entity.setUpdatedAt(action.timestamp());
        }
        interactionRepository.save(entity);
    }

    @KafkaListener(topics = EVENTS_SIMILARITY_TOPIC, containerFactory = "eventSimilarityKafkaListenerContainerFactory")
    @Transactional
    public void handleSimilarity(EventSimilarityAvro similarity) {
        if (similarity == null) {
            return;
        }
        EventPairId id = new EventPairId(similarity.eventA(), similarity.eventB());
        EventSimilarityEntity entity = similarityRepository.findById(id).orElse(null);
        if (entity == null) {
            entity = new EventSimilarityEntity(id, similarity.score(), similarity.timestamp());
        } else {
            entity.setScore(similarity.score());
            entity.setUpdatedAt(similarity.timestamp());
        }
        similarityRepository.save(entity);
    }

    private double weightForAction(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}