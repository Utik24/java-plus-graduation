package ru.practicum.ewm.stats.avro;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.GenericDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UserActionAvroSerializer implements Serializer<UserActionAvro> {
    @Override
    public byte[] serialize(String topic, UserActionAvro data) {
        if (data == null) {
            return null;
        }
        GenericRecord record = new GenericData.Record(AvroSchemas.USER_ACTION_SCHEMA);
        record.put("userId", data.userId());
        record.put("eventId", data.eventId());
        record.put("actionType", new GenericData.EnumSymbol(
                AvroSchemas.USER_ACTION_SCHEMA.getField("actionType").schema(),
                data.actionType().name()
        ));
        record.put("timestamp", data.timestamp().toEpochMilli());

        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(AvroSchemas.USER_ACTION_SCHEMA);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(record, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new SerializationException("Ошибка сериализации UserActionAvro", ex);
        }
    }
}