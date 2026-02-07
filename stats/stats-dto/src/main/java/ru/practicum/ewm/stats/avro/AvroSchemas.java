package ru.practicum.ewm.stats.avro;

import org.apache.avro.Schema;

import java.io.IOException;
import java.io.InputStream;

public final class AvroSchemas {
    public static final Schema USER_ACTION_SCHEMA = loadSchema("avro/user-action.avsc");
    public static final Schema EVENT_SIMILARITY_SCHEMA = loadSchema("avro/event-similarity.avsc");

    private AvroSchemas() {
    }

    private static Schema loadSchema(String resourcePath) {
        try (InputStream input = AvroSchemas.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Не удалось найти схему Avro: " + resourcePath);
            }
            return new Schema.Parser().parse(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось загрузить схему Avro: " + resourcePath, ex);
        }
    }
}