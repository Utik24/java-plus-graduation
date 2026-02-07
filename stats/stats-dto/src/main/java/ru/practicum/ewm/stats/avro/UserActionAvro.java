package ru.practicum.ewm.stats.avro;

import java.time.Instant;

public record UserActionAvro(long userId, long eventId, ActionTypeAvro actionType, Instant timestamp) {
}