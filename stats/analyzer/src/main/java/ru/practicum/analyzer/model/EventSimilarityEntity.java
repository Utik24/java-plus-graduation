package ru.practicum.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "event_similarity")
public class EventSimilarityEntity {

    @EmbeddedId
    private EventPairId id;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EventSimilarityEntity() {
    }

    public EventSimilarityEntity(EventPairId id, double score, Instant updatedAt) {
        this.id = id;
        this.score = score;
        this.updatedAt = updatedAt;
    }

    public EventPairId getId() {
        return id;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}