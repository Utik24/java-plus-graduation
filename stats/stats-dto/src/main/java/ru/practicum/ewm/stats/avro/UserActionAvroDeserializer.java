package ru.practicum.ewm.stats.avro;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.GenericDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.time.Instant;

public class UserActionAvroDeserializer implements Deserializer<UserActionAvro> {
    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(AvroSchemas.USER_ACTION_SCHEMA);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        try {
            GenericRecord record = reader.read(null, decoder);
            long userId = (long) record.get("userId");
            long eventId = (long) record.get("eventId");
            ActionTypeAvro actionType = ActionTypeAvro.valueOf(record.get("actionType").toString());
            long timestamp = (long) record.get("timestamp");
            return new UserActionAvro(userId, eventId, actionType, Instant.ofEpochMilli(timestamp));
        } catch (IOException ex) {
            throw new SerializationException("Ошибка десериализации UserActionAvro", ex);
        }
    }
}