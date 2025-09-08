package irysc.gachesefid.Dto.Deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class MongoNumberLongDeserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isLong() || node.isInt()) {
            // Just a plain number
            return node.asLong();
        } else if (node.isObject()) {
            // Expecting { "$numberLong": "..." }
            JsonNode numberLongNode = node.get("$numberLong");
            if (numberLongNode != null && numberLongNode.isTextual()) {
                try {
                    return Long.parseLong(numberLongNode.asText());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid $numberLong value: " + numberLongNode.asText(), e);
                }
            }
        }

        // Fallback or error
        throw new IOException("Cannot deserialize register_at from JSON: " + node.toString());
    }
}
