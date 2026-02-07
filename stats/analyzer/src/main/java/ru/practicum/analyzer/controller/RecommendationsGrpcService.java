package ru.practicum.analyzer.controller;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.model.UserEventInteractionEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.proto.dashboard.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.dashboard.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.dashboard.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@GrpcService
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    public RecommendationsGrpcService(UserEventInteractionRepository interactionRepository,
                                      EventSimilarityRepository similarityRepository) {
        this.interactionRepository = interactionRepository;
        this.similarityRepository = similarityRepository;
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        List<EventSimilarityEntity> similarities = similarityRepository.findByIdEventAOrIdEventB(eventId, eventId);
        List<RecommendedEventProto> result = similarities.stream()
                .map(similarity -> {
                    long otherEventId = Objects.equals(similarity.getId().getEventA(), eventId)
                            ? similarity.getId().getEventB()
                            : similarity.getId().getEventA();
                    return Map.entry(otherEventId, similarity.getScore());
                })
                .filter(entry -> !interactionRepository.existsByIdUserIdAndIdEventId(userId, entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .toList();

        result.forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        List<UserEventInteractionEntity> recentInteractions = interactionRepository
                .findByIdUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, maxResults));
        if (recentInteractions.isEmpty()) {
            responseObserver.onCompleted();
            return;
        }

        Set<Long> interactedEventIds = interactionRepository.findByIdUserId(userId).stream()
                .map(interaction -> interaction.getId().getEventId())
                .collect(Collectors.toSet());

        Map<Long, Double> candidateScores = new HashMap<>();
        for (UserEventInteractionEntity interaction : recentInteractions) {
            long eventId = interaction.getId().getEventId();
            List<EventSimilarityEntity> similarities = similarityRepository.findByIdEventAOrIdEventB(eventId, eventId);
            for (EventSimilarityEntity similarity : similarities) {
                long otherEventId = Objects.equals(similarity.getId().getEventA(), eventId)
                        ? similarity.getId().getEventB()
                        : similarity.getId().getEventA();
                if (interactedEventIds.contains(otherEventId)) {
                    continue;
                }
                candidateScores.merge(otherEventId, similarity.getScore(), Math::max);
            }
        }

        List<UserEventInteractionEntity> allInteractions = interactionRepository.findByIdUserId(userId);
        List<Map.Entry<Long, Double>> scoredCandidates = candidateScores.keySet().stream()
                .map(candidateEventId -> Map.entry(candidateEventId, predictScore(candidateEventId, allInteractions)))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .toList();

        for (Map.Entry<Long, Double> entry : scoredCandidates) {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(entry.getKey())
                    .setScore(entry.getValue())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        List<Long> eventIds = request.getEventIdList();
        Map<Long, Double> totals = interactionRepository.sumWeightsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(UserEventInteractionRepository.InteractionSum::getEventId,
                        UserEventInteractionRepository.InteractionSum::getTotalWeight));

        for (Long eventId : eventIds) {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(totals.getOrDefault(eventId, 0.0))
                    .build());
        }
        responseObserver.onCompleted();
    }

    private double predictScore(long candidateEventId, List<UserEventInteractionEntity> interactions) {
        if (interactions.isEmpty()) {
            return 0.0;
        }
        int k = Math.min(5, interactions.size());
        List<Neighbor> neighbors = new ArrayList<>();
        for (UserEventInteractionEntity interaction : interactions) {
            long eventId = interaction.getId().getEventId();
            double similarity = findSimilarity(candidateEventId, eventId);
            if (similarity > 0) {
                neighbors.add(new Neighbor(similarity, interaction.getWeight()));
            }
        }
        neighbors.sort(Comparator.comparingDouble(Neighbor::similarity).reversed());
        List<Neighbor> topNeighbors = neighbors.stream().limit(k).toList();

        double weightedSum = 0.0;
        double similaritySum = 0.0;
        for (Neighbor neighbor : topNeighbors) {
            weightedSum += neighbor.similarity() * neighbor.weight();
            similaritySum += neighbor.similarity();
        }
        if (similaritySum == 0.0) {
            return 0.0;
        }
        return weightedSum / similaritySum;
    }

    private double findSimilarity(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        EventSimilarityEntity entity = similarityRepository.findByIdEventAAndIdEventB(first, second);
        return entity != null ? entity.getScore() : 0.0;
    }

    private record Neighbor(double similarity, double weight) {
    }
}