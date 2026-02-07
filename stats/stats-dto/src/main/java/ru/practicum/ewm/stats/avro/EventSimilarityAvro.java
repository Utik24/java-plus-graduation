package ru.practicum.ewm.stats.avro;

import java.time.Instant;

public record EventSimilarityAvro(long eventA, long eventB, double score, Instant timestamp) {
}