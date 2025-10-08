package irysc.gachesefid.Dto.Deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.bson.types.ObjectId;

import java.io.IOException;

public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
    @Override
    public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isTextual()) {
            // Plain hex string case
            String oid = node.asText();
            if (oid == null || oid.isEmpty()) {
                return null;
            }
            return new ObjectId(oid);
        } else if (node.isObject()) {
            // MongoDB extended JSON case: { "$oid": "hexstring" }
            JsonNode oidNode = node.get("$oid");
            if (oidNode != null && oidNode.isTextual()) {
                String oid = oidNode.asText();
                return new ObjectId(oid);
            }
        }

        // fallback - cannot parse ObjectId from this node
        throw new IOException("Cannot deserialize ObjectId from JSON node: " + node.toString());
    }
}
