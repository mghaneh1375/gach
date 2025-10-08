package irysc.gachesefid.Dto.Deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Base64;

public class MongoByteArrayDeserializer extends JsonDeserializer<byte[]> {
    @Override
    public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.has("$binary")) {
            JsonNode binaryNode = node.get("$binary");
            if (binaryNode.isObject() && binaryNode.has("base64")) {
                String base64 = binaryNode.get("base64").asText();
                return Base64.getDecoder().decode(base64);
            }

            // Or sometimes "$binary" is a plain string (v1)
            if (binaryNode.isTextual()) {
                String base64 = binaryNode.asText();
                return Base64.getDecoder().decode(base64);
            }
        }

        // if it's already a base64 string (plain)
        if (node.isTextual()) {
            return Base64.getDecoder().decode(node.asText());
        }

        // if unexpected
        throw new IOException("Cannot deserialize byte[] from node: " + node.toString());
    }
}
