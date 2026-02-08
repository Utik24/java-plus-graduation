package ru.practicum.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_event_interaction")
public class UserEventInteractionEntity {

    @EmbeddedId
    private UserEventId id;

    @Column(name = "weight", nullable = false)
    private double weight;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEventInteractionEntity() {
    }

    public UserEventInteractionEntity(UserEventId id, double weight, Instant updatedAt) {
        this.id = id;
        this.weight = weight;
        this.updatedAt = updatedAt;
    }

    public UserEventId getId() {
        return id;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}