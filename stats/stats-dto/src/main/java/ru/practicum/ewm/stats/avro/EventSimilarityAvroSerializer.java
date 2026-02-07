package ru.practicum.ewm.stats.avro;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EventSimilarityAvroSerializer implements Serializer<EventSimilarityAvro> {
    @Override
    public byte[] serialize(String topic, EventSimilarityAvro data) {
        if (data == null) {
            return null;
        }
        GenericRecord record = new GenericData.Record(AvroSchemas.EVENT_SIMILARITY_SCHEMA);
        record.put("eventA", data.eventA());
        record.put("eventB", data.eventB());
        record.put("score", data.score());
        record.put("timestamp", data.timestamp().toEpochMilli());

        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(AvroSchemas.EVENT_SIMILARITY_SCHEMA);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(record, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new SerializationException("Ошибка сериализации EventSimilarityAvro", ex);
        }
    }
}