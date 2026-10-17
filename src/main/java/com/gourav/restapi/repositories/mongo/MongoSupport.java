package com.gourav.restapi.repositories.mongo;

import com.gourav.restapi.repositories.paging.Pageable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Small helpers shared by the MongoDB-backed repositories.
 */
final class MongoSupport {

    private MongoSupport() {
    }

    /**
     * Identifiers are stored as an {@link ObjectId} when they look like one and as a plain string
     * otherwise, so that hand-seeded documents with readable keys keep working.
     */
    static Object toId(String id) {
        return ObjectId.isValid(id) ? new ObjectId(id) : id;
    }

    static String fromId(Object raw) {
        if (raw == null) {
            return null;
        }
        return raw instanceof ObjectId objectId ? objectId.toHexString() : raw.toString();
    }

    static Instant instant(Document document, String field) {
        Object value = document.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return Instant.parse(value.toString());
    }

    static Date date(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    static Integer integer(Document document, String field) {
        Object value = document.get(field);
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString());
    }

    static Bson sort(Pageable pageable) {
        String property = pageable.getSort().getProperty();
        return pageable.getSort().isAscending() ? Sorts.ascending(property) : Sorts.descending(property);
    }

    /** Case-insensitive "contains" match, mirroring a derived {@code ContainingIgnoreCase} query. */
    static Bson containsIgnoreCase(String field, String value) {
        return Filters.regex(field, ".*" + Pattern.quote(value) + ".*", "i");
    }
}
