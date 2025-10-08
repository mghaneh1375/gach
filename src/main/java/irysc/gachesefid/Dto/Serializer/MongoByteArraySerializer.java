package irysc.gachesefid.Dto.Serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Base64;

public class MongoByteArraySerializer extends JsonSerializer<byte[]> {

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String base64 = Base64.getEncoder().encodeToString(value);

        gen.writeStartObject();
        gen.writeObjectFieldStart("$binary");
        gen.writeStringField("base64", base64);
        gen.writeStringField("subType", "00"); // default subtype for generic binary
        gen.writeEndObject();
        gen.writeEndObject();
    }
}
