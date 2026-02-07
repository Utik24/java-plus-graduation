package ru.practicum.collector;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;

@GrpcService
public class UserActionGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {
    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    public UserActionGrpcService(KafkaTemplate<Long, UserActionAvro> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        UserActionAvro action = new UserActionAvro(
                request.getUserId(),
                request.getEventId(),
                mapAction(request.getActionType()),
                Instant.ofEpochMilli(request.getTimestamp().getSeconds() * 1000 + request.getTimestamp().getNanos() / 1_000_000)
        );
        kafkaTemplate.send(USER_ACTIONS_TOPIC, action.userId(), action);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private ActionTypeAvro mapAction(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Неизвестный тип действия");
        };
    }
}