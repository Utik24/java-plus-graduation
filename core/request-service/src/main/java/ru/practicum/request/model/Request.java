package ru.practicum.request.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "requests", schema = "public")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @Column(name = "created", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false)
    LocalDateTime created;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(name = "requester", nullable = false)
    Long requesterId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    RequestStatus status;
}
