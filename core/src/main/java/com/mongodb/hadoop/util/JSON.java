package com.mongodb.hadoop.util;

import com.mongodb.MongoClientSettings;
import com.mongodb.util.JSONParseException;
import org.bson.BSONCallback;
import org.bson.codecs.Codec;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;

import java.io.StringWriter;

/**
 * Helper methods for JSON serialization and de-serialization
 *
 * @see org.bson.json.JsonReader
 * @see org.bson.json.JsonWriter
 * @see com.mongodb.BasicDBObject#toJson()
 * @see com.mongodb.BasicDBObject#parse(String)
 *
 * @deprecated This class has been superseded by to toJson and parse methods on BasicDBObject
 */
@Deprecated
@SuppressWarnings("deprecation")
public class JSON {

        public static String serialize(Object object) {
                if (object == null)
                        return "null";

                JsonWriter writer = new JsonWriter(new StringWriter(), JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build());
                Codec<Object> codec = (Codec<Object>) MongoClientSettings.getDefaultCodecRegistry().get(object.getClass());
                codec.encode(writer, object, EncoderContext.builder().build());
                return writer.getWriter().toString();
        }

        /**
         * <p>Parses a JSON string and returns a corresponding Java object. The returned value is either a {@link com.mongodb.DBObject DBObject}
         * (if the string is a JSON object or array), or a boxed primitive value according to the following mapping:</p>
         * <ul>
         *     <li>{@code java.lang.Boolean} for {@code true} or {@code false}</li>
         *     <li>{@code java.lang.Integer} for integers between Integer.MIN_VALUE and Integer.MAX_VALUE</li>
         *     <li>{@code java.lang.Long} for integers outside of this range</li>
         *     <li>{@code java.lang.Double} for floating point numbers</li>
         * </ul>
         * If the parameter is a string that contains a single-quoted or double-quoted string, it is returned as an unquoted {@code
         * java.lang.String}. Parses a JSON string representing a JSON value
         *
         * @param jsonString the string to parse
         * @return a Java object representing the JSON data
         * @throws JSONParseException if jsonString is not valid JSON
         */
        public static Object parse(final String jsonString) {
                return null;
        }
}
