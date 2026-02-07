package ru.practicum.ewm.stats.avro;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.DatumReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.time.Instant;

public class EventSimilarityAvroDeserializer implements Deserializer<EventSimilarityAvro> {
    @Override
    public EventSimilarityAvro deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(AvroSchemas.EVENT_SIMILARITY_SCHEMA);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        try {
            GenericRecord record = reader.read(null, decoder);
            long eventA = (long) record.get("eventA");
            long eventB = (long) record.get("eventB");
            double score = (double) record.get("score");
            long timestamp = (long) record.get("timestamp");
            return new EventSimilarityAvro(eventA, eventB, score, Instant.ofEpochMilli(timestamp));
        } catch (IOException ex) {
            throw new SerializationException("Ошибка десериализации EventSimilarityAvro", ex);
        }
    }
}