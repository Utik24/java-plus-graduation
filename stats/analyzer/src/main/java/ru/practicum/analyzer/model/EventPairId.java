package ru.practicum.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EventPairId implements Serializable {
    @Column(name = "event_a")
    private Long eventA;

    @Column(name = "event_b")
    private Long eventB;

    protected EventPairId() {
    }

    public EventPairId(Long eventA, Long eventB) {
        this.eventA = eventA;
        this.eventB = eventB;
    }

    public Long getEventA() {
        return eventA;
    }

    public Long getEventB() {
        return eventB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventPairId that = (EventPairId) o;
        return Objects.equals(eventA, that.eventA) && Objects.equals(eventB, that.eventB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventA, eventB);
    }
}